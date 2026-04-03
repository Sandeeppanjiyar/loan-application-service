package com.example.loan.domain.model;

import com.example.loan.domain.enums.LoanPurpose;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanRequest {

    @Min(10000)
    @Max(5000000)
    private BigDecimal amount;

    @Min(6)
    @Max(360)
    private int tenureMonths;

    @NotNull
    private LoanPurpose purpose;
}
