package com.lam.rentalmanagement.exception;

public class RoleNotFoundException extends RuntimeException{

    public RoleNotFoundException(String roleName){
        super("không tìm thấy vai trò: " + roleName);
    }
}
