package com.lam.rentalmanagement.controller;

import com.lam.rentalmanagement.domain.entity.Role;
import com.lam.rentalmanagement.dto.RoleResponse;
import com.lam.rentalmanagement.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
    @GetMapping
    public ResponseEntity<List<RoleResponse>>  getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }
}
