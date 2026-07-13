package com.lam.rentalmanagement.exception;

public class DuplicateEmailException extends RuntimeException{

    public DuplicateEmailException(String email){
        super("Email đã tồn tại: " + email);
    }
}
