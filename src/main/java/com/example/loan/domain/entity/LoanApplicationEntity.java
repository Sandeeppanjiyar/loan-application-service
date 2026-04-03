
package com.example.loan.domain.entity;

import com.example.loan.domain.enums.ApplicationStatus;
import com.example.loan.domain.enums.RiskBand;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Table(name = "loan_applications")
public class LoanApplicationEntity {

    @Id
    private String applicationId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    private RiskBand riskBand;

    // ---- Offer fields ----
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emi;
    private BigDecimal totalPayable;

    private String rejectionReasons;

    private Instant createdAt;


    protected LoanApplicationEntity() {
    }


    public LoanApplicationEntity(
            String applicationId,
            ApplicationStatus status,
            RiskBand riskBand,
            BigDecimal interestRate,
            Integer tenureMonths,
            BigDecimal emi,
            BigDecimal totalPayable,
            String rejectionReasons
    ) {
        this.applicationId = applicationId;
        this.status = status;
        this.riskBand = riskBand;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
        this.emi = emi;
        this.totalPayable = totalPayable;
        this.rejectionReasons = rejectionReasons;
        this.createdAt = Instant.now();
    }

}