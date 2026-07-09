package com.lam.rentalmanagement.service;


import java.util.List;

import com.lam.rentalmanagement.dto.RoleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lam.rentalmanagement.domain.entity.Role;
import com.lam.rentalmanagement.repository.RoleRepository;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(RoleResponse::from)
                .toList();
    }
}
