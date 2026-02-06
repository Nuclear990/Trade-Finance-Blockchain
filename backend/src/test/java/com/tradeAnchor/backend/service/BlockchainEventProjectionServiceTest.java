package com.tradeAnchor.backend.service;

import com.tradeAnchor.backend.model.LcToken;
import com.tradeAnchor.backend.model.TokenStatus;
import com.tradeAnchor.backend.model.Transaction;
import com.tradeAnchor.backend.repository.BlTokenRepository;
import com.tradeAnchor.backend.repository.LcTokenRepository;
import com.tradeAnchor.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BlockchainEventProjectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private LcTokenRepository lcTokenRepository;
    @Mock
    private BlTokenRepository blTokenRepository;

    private BlockchainEventProjectionService projectionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        projectionService = new BlockchainEventProjectionService(
                transactionRepository,
                lcTokenRepository,
                blTokenRepository);
    }

    @Test
    void onLcIssued_shouldUpdateTokenStatus_whenNotAlreadyIssued() {
        // Arrange
        Long txnId = 123L;
        BigInteger onChainLcId = BigInteger.valueOf(999);

        Transaction mockTxn = new Transaction();
        LcToken mockLcToken = new LcToken();
        mockLcToken.setTokenStatus(TokenStatus.REQUESTED);
        mockTxn.setLcToken(mockLcToken);

        when(transactionRepository.findById(txnId)).thenReturn(Optional.of(mockTxn));

        // Act
        projectionService.onLcIssued(onChainLcId, txnId);

        // Assert
        ArgumentCaptor<LcToken> lcTokenCaptor = ArgumentCaptor.forClass(LcToken.class);
        verify(lcTokenRepository).save(lcTokenCaptor.capture());

        LcToken savedToken = lcTokenCaptor.getValue();
        assertEquals(TokenStatus.ISSUED, savedToken.getTokenStatus());
        assertEquals(onChainLcId, savedToken.getOnChainLcId());
    }
}
