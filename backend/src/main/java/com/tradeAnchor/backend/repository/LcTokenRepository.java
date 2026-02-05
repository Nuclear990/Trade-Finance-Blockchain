package com.tradeAnchor.backend.repository;

import com.tradeAnchor.backend.model.LcToken;
import com.tradeAnchor.backend.model.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LcTokenRepository extends JpaRepository<LcToken, Long> {

    Optional<LcToken> findByOnChainLcId(Long onChainLcId);

    boolean existsByOnChainLcId(Long onChainLcId);

    long countByTokenStatus(TokenStatus tokenStatus);
}
