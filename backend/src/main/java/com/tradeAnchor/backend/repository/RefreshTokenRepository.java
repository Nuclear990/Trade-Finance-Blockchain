package com.tradeAnchor.backend.repository;

import com.tradeAnchor.backend.model.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Transactional
    @Query("""
    update RefreshToken rt
    set rt.revoked = true
    where rt.username = :username
""")
    void revokeAllForUsername(@Param("username") String username);

    @Modifying
    @Transactional
    @Query("""
    update RefreshToken rt
    set rt.revoked = true,
        rt.exp = :expiry
    where rt.username = :username
""")
    void revokeAndExpireAllForUsername(
            @Param("username") String username,
            @Param("expiry") Instant expiry
    );

}
