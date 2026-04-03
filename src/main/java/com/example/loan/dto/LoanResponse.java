package com.example.loan.dto;

import com.example.loan.domain.enums.ApplicationStatus;
import com.example.loan.domain.enums.RejectionReasons;
import com.example.loan.domain.enums.RiskBand;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class LoanResponse {
    private final String applicationId;
    private final ApplicationStatus status;
    private final RiskBand riskBand;
    private final Offer offer;
    private final List<RejectionReasons> rejectionReasons;


    private LoanResponse(String applicationId,
                         ApplicationStatus status,
                         RiskBand riskBand,
                         Offer offer,
                         List<RejectionReasons> rejectionReasons) {
        this.applicationId = applicationId;
        this.status = status;
        this.riskBand = riskBand;
        this.offer = offer;
        this.rejectionReasons = rejectionReasons;
    }

    public LoanResponse approved(RiskBand riskBand, Offer offer){
        return new LoanResponse(
                UUID.randomUUID().toString(),
                ApplicationStatus.APPROVED,
                riskBand,
                offer,
                null
        );
    }

    public LoanResponse rejected(List<RejectionReasons> reasons){
        return new LoanResponse(
                UUID.randomUUID().toString(),
                ApplicationStatus.REJECTED,
                null,
                null,
                reasons
        );
    }

}
