package com.example.loan.service;

import com.example.loan.domain.entity.LoanApplicationEntity;
import com.example.loan.domain.enums.ApplicationStatus;
import com.example.loan.domain.enums.EmploymentType;
import com.example.loan.domain.enums.RejectionReasons;
import com.example.loan.domain.enums.RiskBand;
import com.example.loan.domain.model.Applicant;
import com.example.loan.domain.model.LoanRequest;
import com.example.loan.dto.LoanApplicationRequest;
import com.example.loan.dto.LoanResponse;
import com.example.loan.dto.Offer;
import com.example.loan.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanApplicationRepository loanApplicationRepository;

    public LoanServiceImpl(LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    private static final BigDecimal BASE_RATE_PERCENT = bd("12.0");

    private static final BigDecimal PREMIUM_LOW = bd("0.0");
    private static final BigDecimal PREMIUM_MEDIUM = bd("1.5");
    private static final BigDecimal PREMIUM_HIGH = bd("3.0");

    private static final BigDecimal PREMIUM_SELF_EMPLOYED = bd("1.0");
    private static final BigDecimal PREMIUM_LOAN_GT_10L = bd("0.5");
    private static final BigDecimal TEN_LAKH = bd("1000000");

    private static final BigDecimal SIXTY_PERCENT = bd("0.60");
    private static final BigDecimal FIFTY_PERCENT = bd("0.50");

    private static final BigDecimal ONE_HUNDRED = bd("100");
    private static final BigDecimal TWELVE = bd("12");
    private static final BigDecimal ONE = BigDecimal.ONE;

    // Stability for pow/div
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    @Override
    public LoanResponse processApplication(LoanApplicationRequest request) {

        Applicant applicant = request.getApplicant();
        LoanRequest loan = request.getLoan();

        List<RejectionReasons> reasons = new ArrayList<>();

        // -------- 1) Eligibility Rules (Reject conditions) --------
        if (applicant.getCreditScore() < 600) {
            reasons.add(RejectionReasons.CREDIT_SCORE_TOO_LOW);
        }

        int tenureYearsCeil = (loan.getTenureMonths() + 11) / 12; // ceil months->years
        if (applicant.getAge() + tenureYearsCeil > 65) {
            reasons.add(RejectionReasons.AGE_TENURE_LIMIT_EXCEEDED);
        }

        BigDecimal eligibilityEmi = calculateEmi(loan.getAmount(), loan.getTenureMonths(), BASE_RATE_PERCENT);

        if (eligibilityEmi.compareTo(percentOf(applicant.getMonthlyIncome(), SIXTY_PERCENT)) > 0) {
            reasons.add(RejectionReasons.EMI_EXCEEDS_60_PERCENT);
        }

        if (!reasons.isEmpty()) {
            LoanResponse response = LoanResponse.rejected(reasons);

            // AUDIT SAVE (Rejected)
            saveAudit(response, null, reasons);

            return response;
        }

        // -------- 2) Risk Band Classification --------
        RiskBand riskBand = classifyRiskBand(applicant.getCreditScore());

        // -------- 3) Interest Rate Calculation --------
        BigDecimal finalRatePercent = calculateFinalRatePercent(riskBand, applicant, loan)
                .setScale(2, RoundingMode.HALF_UP);

        // -------- 4) Offer Generation (Single offer for requested tenure) --------
        BigDecimal offerEmi = calculateEmi(loan.getAmount(), loan.getTenureMonths(), finalRatePercent);

        if (offerEmi.compareTo(percentOf(applicant.getMonthlyIncome(), FIFTY_PERCENT)) > 0) {

            List<RejectionReasons> rejectionReasons =
                    List.of(RejectionReasons.EMI_EXCEEDS_50_PERCENT);

            LoanResponse response = LoanResponse.rejected(rejectionReasons);

            // AUDIT SAVE (Rejected)
            saveAudit(response, null, rejectionReasons);

            return response;
        }

        BigDecimal totalPayable = offerEmi
                .multiply(BigDecimal.valueOf(loan.getTenureMonths()))
                .setScale(2, RoundingMode.HALF_UP);

        Offer offer = new Offer(
                finalRatePercent,
                loan.getTenureMonths(),
                offerEmi.setScale(2, RoundingMode.HALF_UP),
                totalPayable
        );

        LoanResponse response = LoanResponse.approved(riskBand, offer);

        // AUDIT SAVE (Approved)
        saveAudit(response, offer, null);

        return response;
    }

    // ------------------ Helpers ------------------

    private RiskBand classifyRiskBand(int score) {
        if (score >= 750) return RiskBand.LOW;
        if (score >= 650) return RiskBand.MEDIUM;
        return RiskBand.HIGH; // 600–649 (since <600 rejected earlier)
    }

    private BigDecimal calculateFinalRatePercent(RiskBand band, Applicant applicant, LoanRequest loan) {
        BigDecimal rate = BASE_RATE_PERCENT;

        switch (band) {
            case LOW -> rate = rate.add(PREMIUM_LOW);
            case MEDIUM -> rate = rate.add(PREMIUM_MEDIUM);
            case HIGH -> rate = rate.add(PREMIUM_HIGH);
        }

        if (applicant.getEmploymentType() == EmploymentType.SELF_EMPLOYED) {
            rate = rate.add(PREMIUM_SELF_EMPLOYED);
        }

        if (loan.getAmount().compareTo(TEN_LAKH) > 0) {
            rate = rate.add(PREMIUM_LOAN_GT_10L);
        }

        return rate;
    }

    /**
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     * r = monthly rate = (annualRate% / 100) / 12
     * scale=2, HALF_UP
     */
    private BigDecimal calculateEmi(BigDecimal principal, int months, BigDecimal annualRatePercent) {
        BigDecimal monthlyRate = annualRatePercent
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(TWELVE, 10, RoundingMode.HALF_UP);

        BigDecimal onePlusR = monthlyRate.add(ONE);
        BigDecimal pow = onePlusR.pow(months, MC);

        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(pow, MC);
        BigDecimal denominator = pow.subtract(ONE, MC);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentOf(BigDecimal value, BigDecimal fraction) {
        return value.multiply(fraction).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /**
     * ✅ Audit save logic (DB persistence)
     *
     * This assumes LoanApplicationEntity has flattened fields:
     * - applicationId, status, riskBand
     * - interestRate, tenureMonths, emi, totalPayable (nullable)
     * - rejectionReasons (String, nullable)
     *
     * If your entity uses different field names, just map accordingly.
     */
    private void saveAudit(LoanResponse response, Offer offer, List<RejectionReasons> reasons) {

        String rejectionReasonsStr = null;
        if (reasons != null && !reasons.isEmpty()) {
            rejectionReasonsStr = reasons.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
        }

        LoanApplicationEntity entity = new LoanApplicationEntity(
                response.getApplicationId(),
                response.getStatus(),          // ApplicationStatus
                response.getRiskBand(),        // RiskBand (null when rejected)
                offer != null ? offer.getInterestRate() : null,
                offer != null ? offer.getTenureMonths() : null,
                offer != null ? offer.getEmi() : null,
                offer != null ? offer.getTotalPayable() : null,
                rejectionReasonsStr
        );

        loanApplicationRepository.save(entity);
    }
}