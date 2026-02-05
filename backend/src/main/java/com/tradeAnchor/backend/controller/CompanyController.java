package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.dto.TokenDto;
import com.tradeAnchor.backend.model.*;
import com.tradeAnchor.backend.repository.BlTokenRepository;
import com.tradeAnchor.backend.repository.LcTokenRepository;
import com.tradeAnchor.backend.repository.TransactionRepository;
import com.tradeAnchor.backend.repository.UsersRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;

@RestController
@RequestMapping("/company")
public class CompanyController {

    private final LcTokenRepository lcTokenRepository;
    private final TransactionRepository transactionRepository;
    private final BlTokenRepository blTokenRepository;
    private final UsersRepository usersRepository;

    public CompanyController(
            LcTokenRepository lcTokenRepository,
            TransactionRepository transactionRepository,
            BlTokenRepository blTokenRepository, UsersRepository usersRepository
    ) {
        this.lcTokenRepository = lcTokenRepository;
        this.transactionRepository = transactionRepository;
        this.blTokenRepository = blTokenRepository;
        this.usersRepository = usersRepository;
    }

    /* ===================================================== */
    /* LC REQUEST                                            */
    /* ===================================================== */

    @Transactional
    @PostMapping("/requestLc")
    public ResponseEntity<?> requestLC(@Valid @RequestBody TokenDto dto) {

        Users company = usersRepository.findById(
                ((Users) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()).getId()
        ).orElseThrow();


        // 1. Create LC intent
        LcToken lc = lcTokenRepository.save(
                new LcToken(null, TokenStatus.REQUESTED)
        );

        // 2. Create transaction (ID generated here)
        Transaction txn = new Transaction(
                null,                          // BL later
                lc,
                company.getEthereumAddress(),  // importer
                dto.getCompany(),              // exporter
                dto.getShipper(),
                dto.getImporterBank(),
                dto.getExporterBank(),
                dto.getAmount(),
                dto.getGoods(),
                TrxnStatus.ACTIVE
        );

        txn = transactionRepository.save(txn);

        Long trxnId = txn.getTrxnId();

// importer (company)
        company.getTrxns().add(trxnId);

// exporter
        Users exporter = usersRepository.findByEthereumAddress(dto.getCompany())
                .orElseThrow(() -> new IllegalStateException("Exporter not found"));
        exporter.getTrxns().add(trxnId);

// shipper
        Users shipper = usersRepository.findByEthereumAddress(dto.getShipper())
                .orElseThrow(() -> new IllegalStateException("Shipper not found"));
        shipper.getTrxns().add(trxnId);

// importer bank
        Users importerBank = usersRepository.findByEthereumAddress(dto.getImporterBank())
                .orElseThrow(() -> new IllegalStateException("Importer bank not found"));
        importerBank.getTrxns().add(trxnId);

// exporter bank
        Users exporterBank = usersRepository.findByEthereumAddress(dto.getExporterBank())
                .orElseThrow(() -> new IllegalStateException("Exporter bank not found"));
        exporterBank.getTrxns().add(trxnId);

        // 3. txn.getId() IS the txnID passed to chain later
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(txn.getTrxnId());
    }

    /* ===================================================== */
    /* BL REQUEST                                            */
    /* ===================================================== */

    @Transactional
    @PostMapping("/requestBl")
    public ResponseEntity<?> requestBL(@RequestParam Long trxnId) {

        Users company = usersRepository.findById(
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

        /* --- ownership check --- */
        if (!company.getEthereumAddress()
                .equalsIgnoreCase(txn.getExporter())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only exporter can request BL");
        }

        if (txn.getBlToken() != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("BL already requested");
        }

        BlToken bl = blTokenRepository.save(
                new BlToken(null, TokenStatus.REQUESTED)
        );

        txn.setBlToken(bl);
        transactionRepository.save(txn);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }
}
