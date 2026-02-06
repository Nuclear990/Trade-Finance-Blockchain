package com.tradeAnchor.backend.repository;

import com.tradeAnchor.backend.model.BlockchainSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockchainSyncRepository extends JpaRepository<BlockchainSyncStatus, String> {
}
