package com.lam.rentalmanagement.exception;

public class UserAccountNotFoundException extends RuntimeException{
    public UserAccountNotFoundException(Long id){
        super("Không thể tìm thấy tài khoản có id:" + id);
    }
}
