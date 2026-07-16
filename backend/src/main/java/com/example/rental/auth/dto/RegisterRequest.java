package com.example.rental.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 150) String fullName,
    @Size(max = 20) String phone
) {
    public RegisterRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email
            + ", password=[REDACTED], fullName=" + fullName
            + ", phone=" + phone + "]";
    }
}
