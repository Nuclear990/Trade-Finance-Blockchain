package com.tradeAnchor.backend.service;

import com.tradeAnchor.backend.model.UserType;
import com.tradeAnchor.backend.model.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.math.BigInteger;
import java.util.List;

@Service
public class BlockchainServiceImpl implements BlockchainService {

    private final VaultService vaultService;
    private final UsersDetailsService usersDetailsService;

    @Value("${blockchain.identity-address}")
    private String identityAddress;

    @Value("${blockchain.manager-address}")
    private String managerAddress;

    @Value("${blockchain.bl-token-address}")
    private String blTokenAddress;

    // tune these once, centrally
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(200_000);
    private static final BigInteger GAS_PRICE =
            BigInteger.valueOf(1_000_000_000L); // 1 gwei

    public BlockchainServiceImpl(VaultService vaultService, UsersDetailsService usersDetailsService) {
        this.vaultService = vaultService;
        this.usersDetailsService = usersDetailsService;
    }

    /* ------------------------------------------------ */
    /* Identity                                         */
    /* ------------------------------------------------ */

    @Override
    public String createUser(Long userId, UserType role) {

        Function fn = new Function(
                "createUser",
                List.of(new Uint8(role.chainValue())),
                List.of()
        );

        return vaultService.signAndSend(
                userId,
                identityAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_LIMIT,
                GAS_PRICE
        );
    }
    public boolean userExists(String username) {

        Users u = (Users) usersDetailsService.loadUserByUsername(username);

        List<TypeReference<?>> outputParams = List.of(
                new TypeReference<Bool>() {}
        );

        Function fn = new Function(
                "getUser",
                List.of(new Address(u.getEthereumAddress())),
                List.of(
                        new TypeReference<Uint8>() {},     // role
                        new TypeReference<Uint256>() {},   // balance
                        new TypeReference<Uint256>() {},   // reserved
                        new TypeReference<Uint256>() {}    // goods
                )
        );

        List<Type> result = vaultService.call(
                identityAddress,
                FunctionEncoder.encode(fn),
                fn.getOutputParameters()
        );

// If all zero, user was never created
        boolean exists =
                !((Uint256) result.get(1)).getValue().equals(BigInteger.ZERO) ||
                        !((Uint256) result.get(2)).getValue().equals(BigInteger.ZERO) ||
                        !((Uint256) result.get(3)).getValue().equals(BigInteger.ZERO);

        return exists;

    }



    @Override
    public String bankPayout(
            Long bankUserId,
            String exporterAddress,
            BigInteger amount
    ) {
        Function fn = new Function(
                "bankPayout",
                List.of(
                        new Address(exporterAddress),
                        new Uint256(amount)
                ),
                List.of()
        );

        return vaultService.signAndSend(
                bankUserId,
                identityAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_LIMIT,
                GAS_PRICE
        );
    }

    /* ------------------------------------------------ */
    /* LC                                               */
    /* ------------------------------------------------ */

    @Override
    public String issueLC(
            Long issuerBankUserId,
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
                issuerBankUserId,
                managerAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_LIMIT,
                GAS_PRICE
        );
    }

    /* ------------------------------------------------ */
    /* BL                                               */
    /* ------------------------------------------------ */

    @Override
    public String issueBL(
            Long shipperUserId,
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
                shipperUserId,
                managerAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_LIMIT,
                GAS_PRICE
        );
    }

    @Override
    public String transferBL(
            Long exporterUserId,
            String exporter,
            String exporterBank,
            BigInteger blTokenId
    ) {
        Function fn = new Function(
                "safeTransferFrom",
                List.of(
                        new Address(exporter),
                        new Address(exporterBank),
                        new Uint256(blTokenId)
                ),
                List.of()
        );

        return vaultService.signAndSend(
                exporterUserId,
                blTokenAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_LIMIT,
                GAS_PRICE
        );
    }

    /* ------------------------------------------------ */
    /* Settlement                                       */
    /* ------------------------------------------------ */

    @Override
    public String settle(
            Long exporterBankUserId,
            BigInteger lcTokenId,
            BigInteger blTokenId
    ) {
        Function fn = new Function(
                "settle",
                List.of(
                        new Uint256(lcTokenId),
                        new Uint256(blTokenId)
                ),
                List.of()
        );

        return vaultService.signAndSend(
                exporterBankUserId,
                managerAddress,
                FunctionEncoder.encode(fn),
                BigInteger.ZERO,
                GAS_LIMIT,
                GAS_PRICE
        );
    }
}
