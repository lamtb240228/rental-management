package com.lam.rentalmanagement.repository;

import com.lam.rentalmanagement.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
