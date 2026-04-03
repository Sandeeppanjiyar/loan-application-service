package com.example.loan.service;

import com.example.loan.dto.LoanApplicationRequest;
import com.example.loan.dto.LoanResponse;

public interface LoanService {
    LoanResponse processApplication(LoanApplicationRequest request);
}
