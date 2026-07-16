package com.example.rental.user.repository;

import com.example.rental.auth.entity.RoleName;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.entity.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsDistinctByRolesNameAndStatusAndDeletedAtIsNull(RoleName roleName, UserStatus status);

    List<UserAccount> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<UserAccount> findByDeletedAtIsNullAndEmailNotInOrderByCreatedAtDesc(Collection<String> emails);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndEmailNotIn(Collection<String> emails);

    long countDistinctByRolesNameAndDeletedAtIsNull(RoleName roleName);

    long countDistinctByRolesNameAndDeletedAtIsNullAndEmailNotIn(RoleName roleName, Collection<String> emails);
}
