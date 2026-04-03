package com.example.loan.controller;

import com.example.loan.domain.model.LoanRequest;
import com.example.loan.dto.LoanApplicationRequest;
import com.example.loan.dto.LoanResponse;
import com.example.loan.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
public class LoanApplicationController {

        @Autowired
        private LoanService loanService;

        @PostMapping
        public ResponseEntity<LoanResponse> create(@RequestBody LoanApplicationRequest request){
            return ResponseEntity.ok(loanService.processApplication(request));
        }
}
