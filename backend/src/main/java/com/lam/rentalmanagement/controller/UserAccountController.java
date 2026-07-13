package com.lam.rentalmanagement.controller;

import java.util.List;

import com.lam.rentalmanagement.dto.CreateUserAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping
    public ResponseEntity<UserAccountResponse> createUserAccount(
            @Valid @RequestBody CreateUserAccountRequest request
    ) {
        UserAccountResponse response =
                userAccountService.createUserAccount(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}