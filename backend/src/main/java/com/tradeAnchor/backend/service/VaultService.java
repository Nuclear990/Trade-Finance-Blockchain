package com.tradeAnchor.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * VaultService
 *
 * - Single backend-controlled Ethereum key (manager / faucet)
 * - Users never sign
 * - All state-changing txs are gas-estimated + buffered
 */
@Service
public class VaultService {

    private static final BigInteger GAS_BUFFER_PERCENT = BigInteger.valueOf(30);

    private final Web3j web3j;
    private final VaultKeyValueOperations kvOps;
    private final Credentials faucetCredentials;

    public VaultService(VaultTemplate vaultTemplate, Web3j web3j) {
        this.web3j = web3j;
        this.kvOps =
                vaultTemplate.opsForKeyValue(
                        "eth-keys",
                        VaultKeyValueOperationsSupport.KeyValueBackend.KV_2
                );
        this.faucetCredentials = loadFaucetCredentials();
    }

    /* ------------------------------------------------ */
    /* Faucet credentials                               */
    /* ------------------------------------------------ */

    private Credentials loadFaucetCredentials() {
        VaultResponse response = kvOps.get("manager");

        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Manager key not found in Vault");
        }

        Object key = response.getData().get("privateKey");

        if (key == null || key.toString().isBlank()) {
            throw new IllegalStateException("privateKey missing in Vault manager entry");
        }

        return Credentials.create(key.toString());
    }

    public String getFaucetAddress() {
        return faucetCredentials.getAddress();
    }

    /* ------------------------------------------------ */
    /* Simulation (revert detection only)               */
    /* ------------------------------------------------ */

    public void simulateOrThrow(String to, String data) {
        try {
            EthCall response =
                    web3j.ethCall(
                            Transaction.createEthCallTransaction(
                                    faucetCredentials.getAddress(),
                                    to,
                                    data
                            ),
                            DefaultBlockParameterName.LATEST
                    ).send();

            if (response.isReverted()) {
                throw new RuntimeException(
                        "SIMULATION REVERTED: " + response.getRevertReason()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Simulation failed", e);
        }
    }

    /* ------------------------------------------------ */
    /* Gas estimation                                   */
    /* ------------------------------------------------ */

    private BigInteger estimateGasOrThrow(String to, String data, BigInteger value) {
        try {
            EthEstimateGas estimate =
                    web3j.ethEstimateGas(
                            Transaction.createFunctionCallTransaction(
                                    faucetCredentials.getAddress(),
                                    null,
                                    null,
                                    null,
                                    to,
                                    value,
                                    data
                            )
                    ).send();

            if (estimate.hasError()) {
                throw new RuntimeException(
                        "Gas estimation failed: " + estimate.getError().getMessage()
                );
            }

            BigInteger base = estimate.getAmountUsed();
            BigInteger buffer =
                    base.multiply(GAS_BUFFER_PERCENT).divide(BigInteger.valueOf(100));

            return base.add(buffer);

        } catch (Exception e) {
            throw new RuntimeException("Gas estimation error", e);
        }
    }

    /* ------------------------------------------------ */
    /* Sign & send (manager only)                       */
    /* ------------------------------------------------ */

    public String signAndSend(
            String to,
            String data,
            BigInteger value,
            BigInteger gasPrice
    ) {

        try {
            // 1. Simulate for logical reverts
            simulateOrThrow(to, data);

            // 2. Estimate gas with buffer
            BigInteger gasLimit = estimateGasOrThrow(to, data, value);

            // 3. Nonce
            BigInteger nonce =
                    web3j.ethGetTransactionCount(
                            faucetCredentials.getAddress(),
                            DefaultBlockParameterName.PENDING
                    ).send().getTransactionCount();

            // 4. Build transaction
            RawTransaction rawTx =
                    RawTransaction.createTransaction(
                            nonce,
                            gasPrice,
                            gasLimit,
                            to,
                            value,
                            data
                    );

            long chainId =
                    web3j.ethChainId()
                            .send()
                            .getChainId()
                            .longValue();

            byte[] signed =
                    TransactionEncoder.signMessage(
                            rawTx,
                            chainId,
                            faucetCredentials
                    );

            // 5. Send
            EthSendTransaction resp =
                    web3j.ethSendRawTransaction(
                            Numeric.toHexString(signed)
                    ).send();

            if (resp.hasError()) {
                throw new RuntimeException(
                        "Ethereum RPC error: " + resp.getError().getMessage()
                );
            }

            String txHash = resp.getTransactionHash();

            // 6. Optional immediate receipt check
            Optional<TransactionReceipt> receiptOpt =
                    web3j.ethGetTransactionReceipt(txHash)
                            .send()
                            .getTransactionReceipt();

            if (receiptOpt.isPresent()) {
                TransactionReceipt r = receiptOpt.get();

                if (!r.isStatusOK()) {
                    if (r.getGasUsed().compareTo(gasLimit) >= 0) {
                        throw new RuntimeException("Transaction ran out of gas");
                    } else {
                        throw new RuntimeException("Transaction reverted");
                    }
                }
            }

            return txHash;

        } catch (Exception e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    /* ------------------------------------------------ */
    /* eth_call (read-only)                             */
    /* ------------------------------------------------ */

    public List<Type> call(
            String to,
            String data,
            List<TypeReference<Type>> outputParams
    ) {

        try {
            EthCall response =
                    web3j.ethCall(
                            Transaction.createEthCallTransaction(
                                    faucetCredentials.getAddress(),
                                    to,
                                    data
                            ),
                            DefaultBlockParameterName.LATEST
                    ).send();

            if (response.isReverted()) {
                throw new RuntimeException(
                        "eth_call reverted: " + response.getRevertReason()
                );
            }

            return FunctionReturnDecoder.decode(
                    response.getValue(),
                    outputParams
            );

        } catch (Exception e) {
            throw new RuntimeException("eth_call failed", e);
        }
    }
}
