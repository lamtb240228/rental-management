package com.lam.rentalmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lam.rentalmanagement.domain.entity.UserAccount;
import com.lam.rentalmanagement.domain.entity.UserAccountStatus;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT userAccount
            FROM UserAccount userAccount
            WHERE (
                :keyword = ''
                OR LOWER(userAccount.email)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(userAccount.fullName)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (
                :status IS NULL
                OR userAccount.status = :status
            )
            ORDER BY userAccount.id ASC
            """)
    List<UserAccount> searchUserAccounts(
            @Param("keyword") String keyword,
            @Param("status") UserAccountStatus status
    );
}