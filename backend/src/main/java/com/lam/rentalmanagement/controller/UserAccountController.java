package com.lam.rentalmanagement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lam.rentalmanagement.dto.UserAccountResponse;
import com.lam.rentalmanagement.service.UserAccountService;

@RestController
@RequestMapping("/api/user-accounts")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ResponseEntity<List<UserAccountResponse>> getAllUserAccounts() {
        return ResponseEntity.ok(
                userAccountService.getAllUserAccounts()
        );
    }
}