package com.tradeAnchor.backend.service;

import com.tradeAnchor.backend.model.UserType;
import com.tradeAnchor.backend.model.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.math.BigInteger;
import java.util.List;

@Service
public class BlockchainService {

    private final VaultService vaultService;
    private final UsersDetailsService usersDetailsService;

    @Value("${blockchain.identity-address}")
    private String identityAddress;

    @Value("${blockchain.manager-address}")
    private String managerAddress;

    @Value("${blockchain.bl-token-address}")
    private String blTokenAddress;

    // Gas price stays here (policy decision)
    private static final BigInteger GAS_PRICE =
            BigInteger.valueOf(1_000_000_000L); // 1 gwei

    public BlockchainService(
            VaultService vaultService,
            UsersDetailsService usersDetailsService
    ) {
        this.vaultService = vaultService;
        this.usersDetailsService = usersDetailsService;
    }

    /* ------------------------------------------------ */
    /* Identity                                         */
    /* ------------------------------------------------ */

    public String createUser(String ethAddress, UserType role) {

        Function fn = new Function(
                "createUser",
                List.of(
                        new Address(ethAddress),
                        new Uint8(role.chainValue())
                ),
                List.of()
        );

        return vaultService.signAndSend(
                identityAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_PRICE
        );
    }

    public boolean userExists(String username) {

        Users u =
                (Users) usersDetailsService.loadUserByUsername(username);

        Function fn = new Function(
                "getUser",
                List.of(new Address(u.getEthereumAddress())),
                List.of(
                        new TypeReference<Uint8>() {},     // role
                        new TypeReference<Uint256>() {},   // balance
                        new TypeReference<Uint256>() {},   // reserved
                        new TypeReference<Uint256>() {},   // goods
                        new TypeReference<Bool>() {}       // exists
                )
        );

        List<Type> result =
                vaultService.call(
                        identityAddress,
                        FunctionEncoder.encode(fn),
                        fn.getOutputParameters()
                );

        return ((Bool) result.get(4)).getValue();
    }

    /* ------------------------------------------------ */
    /* LC                                               */
    /* ------------------------------------------------ */

    public String issueLC(
            String bankActor,
            BigInteger txnId,
            String importer,
            String exporter,
            String issuerBank,
            String beneficiaryBank,
            String shipper,
            BigInteger amount,
            BigInteger goods
    ) {

        Function fn = new Function(
                "issueLC",
                List.of(
                        new Address(bankActor),
                        new Uint256(txnId),
                        new Address(importer),
                        new Address(exporter),
                        new Address(issuerBank),
                        new Address(beneficiaryBank),
                        new Address(shipper),
                        new Uint256(amount),
                        new Uint256(goods)
                ),
                List.of()
        );

        return vaultService.signAndSend(
                managerAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_PRICE
        );
    }

    /* ------------------------------------------------ */
    /* BL                                               */
    /* ------------------------------------------------ */

    public String issueBL(
            String shipperActor,
            BigInteger txnId,
            String importer,
            String exporter,
            String beneficiaryBank,
            String exporterBank,
            String shipper,
            BigInteger goods
    ) {

        Function fn = new Function(
                "issueBL",
                List.of(
                        new Address(shipperActor),
                        new Uint256(txnId),
                        new Address(importer),
                        new Address(exporter),
                        new Address(beneficiaryBank),
                        new Address(exporterBank),
                        new Address(shipper),
                        new Uint256(goods)
                ),
                List.of()
        );

        return vaultService.signAndSend(
                managerAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_PRICE
        );
    }

    /* ------------------------------------------------ */
    /* Settlement                                       */
    /* ------------------------------------------------ */

    public String settle(
            String exporterBankActor,
            BigInteger lcTokenId,
            BigInteger blTokenId
    ) {

        Function fn = new Function(
                "settle",
                List.of(
                        new Address(exporterBankActor),
                        new Uint256(lcTokenId),
                        new Uint256(blTokenId)
                ),
                List.of()
        );

        return vaultService.signAndSend(
                managerAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_PRICE
        );
    }
}
