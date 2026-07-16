package com.lam.rentalmanagement.exception;

public class PasswordConfirmationMismatchException
        extends RuntimeException {

    public PasswordConfirmationMismatchException() {
        super("Mật khẩu xác nhận không khớp");
    }
}