package com.example.rental.user.repository;

import com.example.rental.auth.entity.RoleName;
import com.example.rental.user.entity.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    List<UserAccount> findByDeletedAtIsNullOrderByCreatedAtDesc();

    long countByDeletedAtIsNull();

    long countDistinctByRolesNameAndDeletedAtIsNull(RoleName roleName);
}
