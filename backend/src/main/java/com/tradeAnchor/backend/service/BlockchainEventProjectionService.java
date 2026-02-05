package com.tradeAnchor.backend.service;

import com.tradeAnchor.backend.model.*;
import com.tradeAnchor.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class BlockchainEventProjectionService {

    private final TransactionRepository transactionRepository;
    private final LcTokenRepository lcTokenRepository;
    private final BlTokenRepository blTokenRepository;

    public BlockchainEventProjectionService(
            TransactionRepository transactionRepository,
            LcTokenRepository lcTokenRepository,
            BlTokenRepository blTokenRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.lcTokenRepository = lcTokenRepository;
        this.blTokenRepository = blTokenRepository;
    }

    /* ===================== LC ISSUED ===================== */

    @Transactional
    public void onLcIssued(BigInteger onChainLcId, Long txnId) {
        Transaction txn = transactionRepository
                .findById(txnId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Transaction not found for txnId=" + txnId
                        )
                );

        LcToken lc = txn.getLcToken();

        if (lc.getTokenStatus() == TokenStatus.ISSUED) {
            return; // idempotent
        }

        lc.setOnChainLcId(onChainLcId);
        lc.setTokenStatus(TokenStatus.ISSUED);

        lcTokenRepository.save(lc);
    }

    /* ===================== BL ISSUED ===================== */

    @Transactional
    public void onBlIssued(BigInteger onChainBlId, Long txnId) {

        Transaction txn = transactionRepository
                .findById(txnId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Transaction not found for txnId=" + txnId
                        )
                );

        BlToken bl = txn.getBlToken();

        if (bl == null) {
            throw new IllegalStateException(
                    "BL not requested for txnId=" + txnId
            );
        }

        if (bl.getTokenStatus() == TokenStatus.ISSUED) {
            return; // idempotent
        }

        bl.setOnChainBlId(onChainBlId);
        bl.setTokenStatus(TokenStatus.ISSUED);

        blTokenRepository.save(bl);
    }

    /* ===================== SETTLED ===================== */

    @Transactional
    public void onSettled(Long txnId) {

        Transaction txn = transactionRepository
                .findById(txnId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Transaction not found for txnId=" + txnId
                        )
                );

        if (txn.getTrxnStatus() == TrxnStatus.SETTLED) {
            return; // idempotent
        }

        txn.setTrxnStatus(TrxnStatus.SETTLED);

        txn.getLcToken().setTokenStatus(TokenStatus.UTILIZED);
        txn.getBlToken().setTokenStatus(TokenStatus.UTILIZED);

        transactionRepository.save(txn);
    }
}
