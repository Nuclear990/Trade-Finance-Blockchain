package com.tradeAnchor.backend.repository;

import com.tradeAnchor.backend.model.BlToken;
import com.tradeAnchor.backend.model.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlTokenRepository extends JpaRepository<BlToken, Long> {

    Optional<BlToken> findByOnChainBlId(Long onChainBlId);

    boolean existsByOnChainBlId(Long onChainBlId);

    long countByTokenStatus(TokenStatus tokenStatus);
}
