package com.tradeAnchor.backend.service;

import com.tradeAnchor.backend.event.ManagerEvents;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventValues;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.Contract;

import java.math.BigInteger;

@Service
public class ManagerEventListener {

    private final Web3j web3j;
    private final BlockchainEventProjectionService projectionService;

    @Value("${blockchain.manager-address}")
    private String managerContractAddress;

    public ManagerEventListener(
            Web3j web3j,
            BlockchainEventProjectionService projectionService
    ) {
        this.web3j = web3j;
        this.projectionService = projectionService;
    }

    @PostConstruct
    public void start() {

        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                managerContractAddress
        );

        web3j.ethLogFlowable(filter).subscribe(
                this::handleLog,
                err -> System.err.println("Event listener error: " + err)
        );
    }

    private void handleLog(Log log) {

        String topic0 = log.getTopics().get(0);

        if (topic0.equals(ManagerEvents.LC_ISSUED_TOPIC)) {
            handleLcIssued(log);
        } else if (topic0.equals(ManagerEvents.BL_ISSUED_TOPIC)) {
            handleBlIssued(log);
        } else if (topic0.equals(ManagerEvents.SETTLED_TOPIC)) {
            handleSettled(log);
        }
    }

    /* ---------------- LC ISSUED ---------------- */

    private void handleLcIssued(Log log) {

        EventValues ev =
                Contract.staticExtractEventParameters(
                        ManagerEvents.LC_ISSUED, log
                );

        BigInteger lcId =
                ((org.web3j.abi.datatypes.generated.Uint256)
                        ev.getIndexedValues().get(0)).getValue();

        Long txnId =
                ((org.web3j.abi.datatypes.generated.Uint256)
                        ev.getIndexedValues().get(1)).getValue().longValueExact();

        projectionService.onLcIssued(lcId, txnId);
    }

    /* ---------------- BL ISSUED ---------------- */

    private void handleBlIssued(Log log) {

        EventValues ev =
                Contract.staticExtractEventParameters(
                        ManagerEvents.BL_ISSUED, log
                );

        BigInteger blId =
                ((org.web3j.abi.datatypes.generated.Uint256)
                        ev.getIndexedValues().get(0)).getValue();

        Long txnId =
                ((org.web3j.abi.datatypes.generated.Uint256)
                        ev.getIndexedValues().get(1)).getValue().longValueExact();

        projectionService.onBlIssued(blId, txnId);
    }

    /* ---------------- SETTLED ---------------- */

    private void handleSettled(Log log) {

        EventValues ev =
                Contract.staticExtractEventParameters(
                        ManagerEvents.SETTLED, log
                );

        Long txnId =
                ((org.web3j.abi.datatypes.generated.Uint256)
                        ev.getIndexedValues().get(0)).getValue().longValueExact();

        projectionService.onSettled(txnId);
    }
}
