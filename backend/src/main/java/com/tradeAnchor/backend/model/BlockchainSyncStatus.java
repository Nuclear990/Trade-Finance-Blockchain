package com.tradeAnchor.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigInteger;

@Entity
public class BlockchainSyncStatus {

    @Id
    private String id; // e.g., "MANAGER_CONTRACT"

    private BigInteger lastBlockNumber;

    public BlockchainSyncStatus() {
    }

    public BlockchainSyncStatus(String id, BigInteger lastBlockNumber) {
        this.id = id;
        this.lastBlockNumber = lastBlockNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigInteger getLastBlockNumber() {
        return lastBlockNumber;
    }

    public void setLastBlockNumber(BigInteger lastBlockNumber) {
        this.lastBlockNumber = lastBlockNumber;
    }
}
