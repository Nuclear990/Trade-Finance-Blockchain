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
        @Value("${blockchain.max-block-range:10}")
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

        @Scheduled(fixedDelayString = "${blockchain.polling-interval-ms:15000}")
        public void pollEvents() {
                try {
                        BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
                        if (currentBlock == null) {
                                logger.error("Failed to fetch current block number");
                                return;
                        }

                        BigInteger lastBlock = syncRepository.findById(SYNC_ID)
                                        .map(BlockchainSyncStatus::getLastBlockNumber)
                                        .orElse(null);

                        if (lastBlock == null) {
                                // First run: establish baseline or start from LATEST-ish?
                                // Depending on requirements. Here safely defaulting to currentBlock - 1 to
                                // start listening from now.
                                // Or if we want history, we should have started with 0 or a deployment block.
                                // Given previous logic was LATEST, starting from currentBlock is safer default.
                                logger.info("No sync status found. Initializing sync from block {}", currentBlock);
                                updateLastBlock(currentBlock);
                                return;
                        }

                        if (currentBlock.compareTo(lastBlock) <= 0) {
                                return; // Nothing new
                        }

                        BigInteger fromBlock = lastBlock.add(BigInteger.ONE);
                        BigInteger toBlock = currentBlock;

                        // Chunking
                        if (toBlock.subtract(fromBlock).compareTo(BigInteger.valueOf(maxBlockRange - 1)) > 0) {
                                toBlock = fromBlock.add(BigInteger.valueOf(maxBlockRange - 1));
                        }

                        fetchAndProcessLogs(fromBlock, toBlock);
                        updateLastBlock(toBlock);

                } catch (Exception e) {
                        logger.error("Error during blockchain polling", e);
                }
        }

        private void fetchAndProcessLogs(BigInteger from, BigInteger to) throws IOException {
                logger.debug("Polling logs from {} to {}", from, to);

                EthFilter filter = new EthFilter(
                                new DefaultBlockParameterNumber(from),
                                new DefaultBlockParameterNumber(to),
                                managerContractAddress);

                // eth_getLogs
                EthLog ethLog = web3j.ethGetLogs(filter).send();

                if (ethLog.hasError()) {
                        throw new IOException("Error fetching logs: " + ethLog.getError().getMessage());
                }

                List<EthLog.LogResult> logs = ethLog.getLogs();
                for (EthLog.LogResult logResult : logs) {
                        // LogResult can be a Hash or a LogObject. ethGetLogs usually returns LogObject
                        if (logResult instanceof EthLog.LogObject) {
                                Log log = ((EthLog.LogObject) logResult).get();
                                handleLog(log);
                        }
                }
        }

        private void handleLog(Log log) {
                try {
                        logger.debug("Received log: block={}, txn={}", log.getBlockNumber(), log.getTransactionHash());

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

                } catch (Exception e) {
                        logger.error("Error processing log: {}", log, e);
                }
        }

        private void updateLastBlock(BigInteger blockNumber) {
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
