package com.lam.rentalmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserAccountRequest (

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
        String email,

        @NotBlank(message ="Mật khẩu không được để trống")
        @Size(
                min = 8,
                max = 72,
                message = " Mật khẩu phải có từ 8 đến 72 ký tự"
        )
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150,message = "Họ tên không được vượt quá 150 ký tự")
        String fullName,

        @Pattern(
                regexp = "^$|^\\+?[0-9]{9,15}$",
                message = "Số điện thoại phải có từ 9 đến 15 chữ số"
        )
        String phone,

        @NotBlank(message = "Vai trò không được để trống")
        String roleName
) {
}
