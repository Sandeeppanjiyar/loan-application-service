package com.example.loan.dto;

import com.example.loan.domain.model.Applicant;
import com.example.loan.domain.model.LoanRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationRequest {

    @Valid
    @NotNull
    private Applicant applicant;


    @Valid
    @NotNull
    private LoanRequest loan;
}
