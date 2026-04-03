package com.example.loan.domain.model;

import com.example.loan.domain.enums.EmploymentType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Applicant {

    @NotBlank
    private String name;

    @Min(21)
    @Max(60)
    private int age;

    @Positive
    private BigDecimal monthlyIncome;

    @NotNull
    private EmploymentType employmentType;

    @Min(300)
    @Max(900)
    private int creditScore;
}
