package com.example.rental.auth.repository;

import com.example.rental.auth.entity.RefreshSession;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshSession session where session.tokenHash = :tokenHash")
    Optional<RefreshSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("""
        update RefreshSession session
        set session.revokedAt = :revokedAt
        where session.familyId = :familyId
          and session.revokedAt is null
        """)
    int revokeActiveFamily(@Param("familyId") UUID familyId, @Param("revokedAt") OffsetDateTime revokedAt);

    @Modifying(flushAutomatically = true)
    @Query("""
        update RefreshSession session
        set session.revokedAt = :revokedAt
        where session.userAccount.id = :userAccountId
          and session.revokedAt is null
        """)
    int revokeAllActiveForUser(
        @Param("userAccountId") Long userAccountId,
        @Param("revokedAt") OffsetDateTime revokedAt
    );
}
