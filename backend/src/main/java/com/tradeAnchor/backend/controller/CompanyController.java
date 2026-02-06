package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.dto.TokenDto;
import com.tradeAnchor.backend.exception.ConflictException;
import com.tradeAnchor.backend.exception.ForbiddenException;
import com.tradeAnchor.backend.exception.ResourceNotFoundException;
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
                        BlTokenRepository blTokenRepository, UsersRepository usersRepository) {
                this.lcTokenRepository = lcTokenRepository;
                this.transactionRepository = transactionRepository;
                this.blTokenRepository = blTokenRepository;
                this.usersRepository = usersRepository;
        }

        private Users getCurrentUser() {
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof Users) {
                        return usersRepository.findById(((Users) principal).getId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
                }
                throw new ForbiddenException("User not authenticated properly");
        }

        private Users resolveUser(String username, String roleName) {
                return usersRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(roleName + " not found: " + username));
        }

        /* ===================================================== */
        /* LC REQUEST */
        /* ===================================================== */

        @Transactional
        @PostMapping("/requestLc")
        public ResponseEntity<?> requestLC(@Valid @RequestBody TokenDto dto) {
                Users company = getCurrentUser();

                // Resolve Users from Usernames
                Users exporter = resolveUser(dto.getCompany(), "Exporter");
                Users shipper = resolveUser(dto.getShipper(), "Shipper");
                Users importerBank = resolveUser(dto.getImporterBank(), "Importer Bank");
                Users exporterBank = resolveUser(dto.getExporterBank(), "Exporter Bank");

                // 1. Create LC intent
                LcToken lc = lcTokenRepository.save(new LcToken(null, TokenStatus.REQUESTED));

                // 2. Create transaction (ID generated here)
                Transaction txn = new Transaction(
                                null, // BL later
                                lc,
                                company.getEthereumAddress(), // importer
                                exporter.getEthereumAddress(),
                                shipper.getEthereumAddress(),
                                importerBank.getEthereumAddress(),
                                exporterBank.getEthereumAddress(),
                                dto.getAmount(),
                                dto.getGoods(),
                                TrxnStatus.ACTIVE);

                txn = transactionRepository.save(txn);

                Long trxnId = txn.getTrxnId();

                // associated users
                company.getTrxns().add(trxnId);
                exporter.getTrxns().add(trxnId);
                shipper.getTrxns().add(trxnId);
                importerBank.getTrxns().add(trxnId);
                exporterBank.getTrxns().add(trxnId);

                // 3. txn.getId() IS the txnID passed to chain later
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(txn.getTrxnId());
        }

        /* ===================================================== */
        /* BL REQUEST */
        /* ===================================================== */

        @Transactional
        @PostMapping("/requestBl")
        public ResponseEntity<?> requestBL(@RequestParam Long trxnId) {
                Users company = getCurrentUser();

                Transaction txn = transactionRepository.findById(trxnId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found for id=" + trxnId));

                /* --- ownership check --- */
                if (!company.getEthereumAddress().equalsIgnoreCase(txn.getExporter())) {
                        throw new ForbiddenException("Only exporter can request BL");
                }

                if (txn.getBlToken() != null) {
                        throw new ConflictException("BL already requested");
                }

                BlToken bl = blTokenRepository.save(new BlToken(null, TokenStatus.REQUESTED));

                txn.setBlToken(bl);
                transactionRepository.save(txn);

                return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        /* ===================================================== */
        /* DATA FETCHING */
        /* ===================================================== */

        @GetMapping("/myLcs")
        public ResponseEntity<List<Transaction>> getMyLcs() {
                Users company = getCurrentUser();
                // Assumption: Importer requests LCs. So user is Importer.
                List<Transaction> txns = transactionRepository.findByImporter(company.getEthereumAddress());
                return ResponseEntity.ok(txns);
        }

        @GetMapping("/myBls")
        public ResponseEntity<List<Transaction>> getMyBls() {
                Users company = getCurrentUser();
                // Exporter deals with BLs usually (requests them).
                List<Transaction> txns = transactionRepository.findByExporter(company.getEthereumAddress());
                return ResponseEntity.ok(txns);
        }
}
