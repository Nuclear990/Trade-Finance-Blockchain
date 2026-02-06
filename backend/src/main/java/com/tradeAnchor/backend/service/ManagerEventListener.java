package com.tradeAnchor.backend.service;

import com.tradeAnchor.backend.event.ManagerEvents;
import com.tradeAnchor.backend.model.BlockchainSyncStatus;
import com.tradeAnchor.backend.repository.BlockchainSyncRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventValues;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.Contract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Service
public class ManagerEventListener {

        private static final Logger logger = LoggerFactory.getLogger(ManagerEventListener.class);
        private static final String SYNC_ID = "MANAGER_CONTRACT";

        @Value("${blockchain.max-block-range:2000}")
        private int maxBlockRange;

        private final Web3j web3j;
        private final BlockchainEventProjectionService projectionService;
        private final BlockchainSyncRepository syncRepository;

        @Value("${blockchain.manager-address}")
        private String managerContractAddress;

        public ManagerEventListener(
                        Web3j web3j,
                        BlockchainEventProjectionService projectionService,
                        BlockchainSyncRepository syncRepository) {
                this.web3j = web3j;
                this.projectionService = projectionService;
                this.syncRepository = syncRepository;
        }

        @PostConstruct
        public void init() {
                // Run initial catch-up in a separate thread to avoid blocking startup
                new Thread(this::catchUpAndSubscribe).start();
        }

        private void catchUpAndSubscribe() {
                try {
                        BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
                        if (currentBlock == null) {
                                logger.error("Failed to fetch current block number");
                                return;
                        }

                        BigInteger lastBlock = syncRepository.findById(SYNC_ID)
                                        .map(BlockchainSyncStatus::getLastBlockNumber)
                                        .orElse(currentBlock.subtract(BigInteger.valueOf(100))); // Default to recent
                                                                                                 // history if fresh

                        logger.info("Starting catch-up form block {} to {}", lastBlock, currentBlock);

                        // Free Tier Specfic: If we are behind by more than 10 blocks, we cannot
                        // efficiently catch up
                        // without hitting rate limits or "range too large" errors (10 block max).
                        // User requested "change architecture... get notified", implying we should
                        // prioritize live events
                        // over historical consistency if polling is broken.
                        long gap = currentBlock.subtract(lastBlock).longValue();
                        if (gap > 10) {
                                logger.warn("Block gap ({} blocks) exceeds Free Tier polling limit (10). Skipping historical sync to avoid errors. Will start listening for NEW events from block {}.",
                                                gap, currentBlock);
                                lastBlock = currentBlock;
                                updateLastBlock(lastBlock);
                        } else {
                                // Small gap? Try to fetch it.
                                if (gap > 0) {
                                        fetchAndProcessLogs(lastBlock.add(BigInteger.ONE), currentBlock);
                                        lastBlock = currentBlock;
                                        updateLastBlock(lastBlock);
                                }
                        }

                        logger.info("Catch-up complete. Subscribing to new events...");
                        subscribeToEvents(currentBlock);

                } catch (Exception e) {
                        logger.error("Error during blockchain sync/subscription", e);
                }
        }

        private void subscribeToEvents(BigInteger startBlock) {
                EthFilter filter = new EthFilter(
                                DefaultBlockParameter.valueOf(startBlock),
                                DefaultBlockParameterName.LATEST,
                                managerContractAddress);

                web3j.ethLogFlowable(filter).subscribe(
                                this::handleLog,
                                error -> logger.error("Error in event subscription", error));
        }

        private void fetchAndProcessLogs(BigInteger from, BigInteger to) throws IOException {
                logger.debug("Polling logs from {} to {}", from, to);

                EthFilter filter = new EthFilter(
                                new DefaultBlockParameterNumber(from),
                                new DefaultBlockParameterNumber(to),
                                managerContractAddress);

                EthLog ethLog = web3j.ethGetLogs(filter).send();

                if (ethLog.hasError()) {
                        throw new IOException("Error fetching logs: " + ethLog.getError().getMessage());
                }

                List<EthLog.LogResult> logs = ethLog.getLogs();
                for (EthLog.LogResult logResult : logs) {
                        if (logResult instanceof EthLog.LogObject) {
                                Log log = ((EthLog.LogObject) logResult).get();
                                handleLog(log);
                        }
                }
        }

        private void handleLog(Log log) {
                try {
                        logger.info("Received log: block={}, txn={}", log.getBlockNumber(), log.getTransactionHash());

                        List<String> topics = log.getTopics();
                        if (topics == null || topics.isEmpty())
                                return;

                        String topic0 = topics.get(0);

                        if (topic0.equals(ManagerEvents.LC_ISSUED_TOPIC)) {
                                handleLcIssued(log);
                        } else if (topic0.equals(ManagerEvents.BL_ISSUED_TOPIC)) {
                                handleBlIssued(log);
                        } else if (topic0.equals(ManagerEvents.SETTLED_TOPIC)) {
                                handleSettled(log);
                        }

                        // For subscription updates, we might want to periodically update last block,
                        // or update it per event. Here we update if it's greater than stored.
                        // However, flowable doesn't guarantee order if we process async...
                        // but this is synchronous handling.
                        // Ideally only update last block if we are consistently syncing?
                        // For now, let's trust the subscription stream and catch-up mechanism next
                        // time.
                        updateLastBlock(log.getBlockNumber());

                } catch (Exception e) {
                        logger.error("Error processing log: {}", log, e);
                }
        }

        private void updateLastBlock(BigInteger blockNumber) {
                // Optimize: Don't save on every single log if high volume, but for this use
                // case it's likely fine.
                BlockchainSyncStatus status = new BlockchainSyncStatus(SYNC_ID, blockNumber);
                syncRepository.save(status);
        }

        /* ---------------- LC ISSUED ---------------- */

        private void handleLcIssued(Log log) {
                logger.info("Processing LC_ISSUED event");
                EventValues ev = Contract.staticExtractEventParameters(ManagerEvents.LC_ISSUED, log);

                BigInteger lcId = ((org.web3j.abi.datatypes.generated.Uint256) ev.getIndexedValues().get(0)).getValue();
                Long txnId = ((org.web3j.abi.datatypes.generated.Uint256) ev.getIndexedValues().get(1)).getValue()
                                .longValueExact();

                projectionService.onLcIssued(lcId, txnId);
        }

        /* ---------------- BL ISSUED ---------------- */

        private void handleBlIssued(Log log) {
                logger.info("Processing BL_ISSUED event");
                EventValues ev = Contract.staticExtractEventParameters(ManagerEvents.BL_ISSUED, log);

                BigInteger blId = ((org.web3j.abi.datatypes.generated.Uint256) ev.getIndexedValues().get(0)).getValue();
                Long txnId = ((org.web3j.abi.datatypes.generated.Uint256) ev.getIndexedValues().get(1)).getValue()
                                .longValueExact();

                projectionService.onBlIssued(blId, txnId);
        }

        /* ---------------- SETTLED ---------------- */

        private void handleSettled(Log log) {
                logger.info("Processing SETTLED event");
                EventValues ev = Contract.staticExtractEventParameters(ManagerEvents.SETTLED, log);

                Long txnId = ((org.web3j.abi.datatypes.generated.Uint256) ev.getIndexedValues().get(0)).getValue()
                                .longValueExact();

                projectionService.onSettled(txnId);
        }
}
