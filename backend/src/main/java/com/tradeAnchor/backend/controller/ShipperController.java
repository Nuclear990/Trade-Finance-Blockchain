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
@RequestMapping("/shipper")
public class ShipperController {

    private final BlockchainService blockchainService;
    private final TransactionRepository transactionRepository;
    private final UsersRepository usersRepository;
    public ShipperController(
            BlockchainService blockchainService,
            TransactionRepository transactionRepository, UsersRepository usersRepository
    ) {
        this.blockchainService = blockchainService;
        this.transactionRepository = transactionRepository;
        this.usersRepository = usersRepository;
    }

    /* ===================================================== */
    /* BL ISSUANCE                                           */
    /* ===================================================== */

    @PostMapping("/issue-bl")
    public ResponseEntity<?> issueBL(@RequestParam Long trxnId) {

        Users shipper = usersRepository.findById(
                ((Users) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()).getId()
        ).orElseThrow();

        Transaction txn = transactionRepository
                .findById(trxnId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found for id=" + trxnId
                        )
                );

        /* ---- authorization ---- */
        if (!shipper.getEthereumAddress()
                .equalsIgnoreCase(txn.getShipper())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not the shipper for this transaction");
        }

        /* ---- state validation ---- */
        if (txn.getBlToken() == null ||
                txn.getBlToken().getTokenStatus() != TokenStatus.REQUESTED) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("BL is not in REQUESTED state");
        }

        if (txn.getLcToken() == null ||
                txn.getLcToken().getTokenStatus() != TokenStatus.ISSUED) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("LC must be ISSUED before BL issuance");
        }

        /* ---- trigger on-chain issuance ---- */
        blockchainService.issueBL(
                shipper.getEthereumAddress(),
                BigInteger.valueOf(txn.getTrxnId()),                 // DB-generated txnID
                txn.getImporter(),
                txn.getExporter(),
                txn.getImporterBank(),
                txn.getExporterBank(),
                txn.getShipper(),
                txn.getGoods()
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }
}
