package com.example.rental.auth.repository;

import com.example.rental.auth.entity.Role;
import com.example.rental.auth.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
