package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.exception.ConflictException;
import com.tradeAnchor.backend.exception.ForbiddenException;
import com.tradeAnchor.backend.exception.ResourceNotFoundException;
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
import java.util.List;

@RestController
@RequestMapping("/bank")
public class BankController {

        private final BlockchainService blockchainService;
        private final TransactionRepository transactionRepository;
        private final UsersRepository usersRepository;

        public BankController(
                        BlockchainService blockchainService,
                        TransactionRepository transactionRepository, UsersRepository usersRepository) {
                this.blockchainService = blockchainService;
                this.transactionRepository = transactionRepository;
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

        /* ===================================================== */
        /* LC ISSUANCE */
        /* ===================================================== */

        @PostMapping("/issueLc")
        public ResponseEntity<?> issueLC(@RequestParam Long trxnId) {
                Users bank = getCurrentUser();

                Transaction trxn = transactionRepository.findById(trxnId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found for TrxnId=" + trxnId));

                /* ---- authorization ---- */
                if (!bank.getEthereumAddress().equalsIgnoreCase(trxn.getImporterBank())) {
                        throw new ForbiddenException("You are not the importer bank for this transaction");
                }

                /* ---- state validation ---- */
                if (trxn.getLcToken() == null ||
                                (trxn.getLcToken().getTokenStatus() != TokenStatus.REQUESTED &&
                                                trxn.getLcToken().getTokenStatus() != TokenStatus.FAILED)) {
                        throw new ConflictException("LC must be in REQUESTED or FAILED state");
                }

                /* ---- set PROCESSING status ---- */
                trxn.getLcToken().setTokenStatus(TokenStatus.PROCESSING);
                transactionRepository.save(trxn);

                /* ---- trigger on-chain issuance ---- */
                try {
                        blockchainService.issueLC(
                                        bank.getEthereumAddress(),
                                        BigInteger.valueOf(trxnId),
                                        trxn.getImporter(),
                                        trxn.getExporter(),
                                        trxn.getImporterBank(),
                                        trxn.getExporterBank(),
                                        trxn.getShipper(),
                                        trxn.getAmount(),
                                        trxn.getGoods());

                        // async blockchain call → ACCEPTED, not OK
                        return ResponseEntity.status(HttpStatus.ACCEPTED).build();

                } catch (Exception e) {
                        // Revert to FAILED on error
                        trxn.getLcToken().setTokenStatus(TokenStatus.FAILED);
                        transactionRepository.save(trxn);

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Failed to issue LC. Please try again later.");
                }
        }

        /* ===================================================== */
        /* SETTLEMENT */
        /* ===================================================== */

        @PostMapping("/settle")
        public ResponseEntity<?> settle(@RequestParam Long trxnId) {
                Users bank = getCurrentUser();

                Transaction trxn = transactionRepository.findById(trxnId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found for TrxnId=" + trxnId));

                /* ---- authorization ---- */
                if (!bank.getEthereumAddress().equalsIgnoreCase(trxn.getExporterBank())) {
                        throw new ForbiddenException("You are not the exporter bank for this transaction");
                }

                /* ---- state validation ---- */
                if (trxn.getLcToken() == null ||
                                trxn.getBlToken() == null ||
                                trxn.getLcToken().getTokenStatus() != TokenStatus.ISSUED ||
                                trxn.getBlToken().getTokenStatus() != TokenStatus.ISSUED) {
                        throw new ConflictException("Transaction is not ready for settlement");
                }

                /* ---- trigger on-chain settlement ---- */
                blockchainService.settle(
                                bank.getEthereumAddress(),
                                trxn.getLcToken().getOnChainLcId(),
                                trxn.getBlToken().getOnChainBlId());

                return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        /* ===================================================== */
        /* DATA FETCHING */
        /* ===================================================== */

        @GetMapping("/pendingLcs")
        public ResponseEntity<List<Transaction>> getPendingLcs() {
                Users bank = getCurrentUser();
                List<Transaction> txns = transactionRepository
                                .findByImporterBankAndLcToken_TokenStatus(
                                                bank.getEthereumAddress(),
                                                TokenStatus.REQUESTED);
                return ResponseEntity.ok(txns);
        }

        @GetMapping("/issuedLcs")
        public ResponseEntity<List<Transaction>> getIssuedLcs() {
                // Simplicity: Show all ISSUED LCs.
                List<Transaction> txns = transactionRepository
                                .findByLcToken_TokenStatus(TokenStatus.ISSUED);
                return ResponseEntity.ok(txns);
        }

        @GetMapping("/settleableTrades")
        public ResponseEntity<List<Transaction>> getSettleableTrades() {
                Users bank = getCurrentUser();
                // Exporter Bank settles trades. Needs LC=ISSUED, BL=ISSUED.
                List<Transaction> txns = transactionRepository
                                .findByExporterBankAndLcToken_TokenStatusAndBlToken_TokenStatus(
                                                bank.getEthereumAddress(),
                                                TokenStatus.ISSUED,
                                                TokenStatus.ISSUED);
                return ResponseEntity.ok(txns);
        }
}
