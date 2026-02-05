package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.model.TokenStatus;
import com.tradeAnchor.backend.model.Transaction;
import com.tradeAnchor.backend.model.Users;
import com.tradeAnchor.backend.repository.TransactionRepository;
import com.tradeAnchor.backend.repository.UsersRepository;
import com.tradeAnchor.backend.service.BlockchainService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

@RestController
@RequestMapping("/bank")
public class BankController {

    private final BlockchainService blockchainService;
    private final TransactionRepository transactionRepository;
    private final UsersRepository usersRepository;
    public BankController(
            BlockchainService blockchainService,
            TransactionRepository transactionRepository, UsersRepository usersRepository
    ) {
        this.blockchainService = blockchainService;
        this.transactionRepository = transactionRepository;
        this.usersRepository = usersRepository;
    }

    /* ===================================================== */
    /* LC ISSUANCE                                           */
    /* ===================================================== */

    @PostMapping("/issueLc")
    public ResponseEntity<?> issueLC(@RequestParam Long trxnId) {

        Users bank = usersRepository.findById(
                ((Users) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()).getId()
        ).orElseThrow();


        Transaction trxn = transactionRepository
                .findById(trxnId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found for TrxnId=" + trxnId
                        )
                );

        /* ---- authorization ---- */
        if (!bank.getEthereumAddress()
                .equalsIgnoreCase(trxn.getImporterBank())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not the importer bank for this transaction");
        }

        /* ---- state validation ---- */
        if (trxn.getLcToken() == null ||
                trxn.getLcToken().getTokenStatus() != TokenStatus.REQUESTED) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("LC is not in REQUESTED state");
        }

        /* ---- trigger on-chain issuance ---- */
        blockchainService.issueLC(
                bank.getEthereumAddress(),
                BigInteger.valueOf(trxnId),
                trxn.getImporter(),
                trxn.getExporter(),
                trxn.getImporterBank(),
                trxn.getExporterBank(),
                trxn.getShipper(),
                trxn.getAmount(),
                trxn.getGoods()
        );

        // async blockchain call → ACCEPTED, not OK
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    /* ===================================================== */
    /* SETTLEMENT                                            */
    /* ===================================================== */

    @PostMapping("/settle")
    public ResponseEntity<?> settle(@RequestParam Long trxnId) {

        Users bank = usersRepository.findById(
                ((Users) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()).getId()
        ).orElseThrow();

        Transaction trxn = transactionRepository
                .findById(trxnId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found for TrxnId=" + trxnId
                        )
                );

        /* ---- authorization ---- */
        if (!bank.getEthereumAddress()
                .equalsIgnoreCase(trxn.getExporterBank())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not the exporter bank for this transaction");
        }

        /* ---- state validation ---- */
        if (trxn.getLcToken() == null ||
                trxn.getBlToken() == null ||
                trxn.getLcToken().getTokenStatus() != TokenStatus.ISSUED ||
                trxn.getBlToken().getTokenStatus() != TokenStatus.ISSUED) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Transaction is not ready for settlement");
        }

        /* ---- trigger on-chain settlement ---- */
        blockchainService.settle(
                bank.getEthereumAddress(),
                trxn.getLcToken().getOnChainLcId(),
                trxn.getBlToken().getOnChainBlId()
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }
}
