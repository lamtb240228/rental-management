package com.lam.rentalmanagement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lam.rentalmanagement.dto.ChangePasswordRequest;
import com.lam.rentalmanagement.dto.CreateUserAccountRequest;
import com.lam.rentalmanagement.dto.UpdateUserAccountRequest;
import com.lam.rentalmanagement.dto.UserAccountResponse;
import com.lam.rentalmanagement.service.UserAccountService;

import jakarta.validation.Valid;

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

    @GetMapping("/{id}")
    public ResponseEntity<UserAccountResponse> getUserAccountById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                userAccountService.getUserAccountById(id)
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

    @PutMapping("/{id}")
    public ResponseEntity<UserAccountResponse> updateUserAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserAccountRequest request
    ) {
        return ResponseEntity.ok(
                userAccountService.updateUserAccount(id, request)
        );
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userAccountService.changePassword(id, request);

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> lockUserAccount(
            @PathVariable Long id
    ) {
        userAccountService.lockUserAccount(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<Void> unlockUserAccount(
            @PathVariable Long id
    ) {
        userAccountService.unlockUserAccount(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}