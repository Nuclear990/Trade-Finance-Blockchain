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

@RestController
@RequestMapping("/shipper")
public class ShipperController {

        private final BlockchainService blockchainService;
        private final TransactionRepository transactionRepository;
        private final UsersRepository usersRepository;

        public ShipperController(
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
        /* BL ISSUANCE */
        /* ===================================================== */

        @PostMapping("/issueBl")
        public ResponseEntity<?> issueBL(@RequestParam Long trxnId) {
                Users shipper = getCurrentUser();

                Transaction txn = transactionRepository.findById(trxnId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found for id=" + trxnId));

                /* ---- authorization ---- */
                if (!shipper.getEthereumAddress().equalsIgnoreCase(txn.getShipper())) {
                        throw new ForbiddenException("You are not the shipper for this transaction");
                }

                /* ---- state validation ---- */
                if (txn.getBlToken() == null ||
                                (txn.getBlToken().getTokenStatus() != TokenStatus.REQUESTED &&
                                                txn.getBlToken().getTokenStatus() != TokenStatus.FAILED)) {
                        throw new ConflictException("BL must be in REQUESTED or FAILED state");
                }

                if (txn.getLcToken() == null ||
                                txn.getLcToken().getTokenStatus() != TokenStatus.ISSUED) {
                        throw new ConflictException("LC must be ISSUED before BL issuance");
                }

                /* ---- set PROCESSING status ---- */
                txn.getBlToken().setTokenStatus(TokenStatus.PROCESSING);
                transactionRepository.save(txn);

                /* ---- trigger on-chain issuance ---- */
                try {
                        blockchainService.issueBL(
                                        shipper.getEthereumAddress(),
                                        BigInteger.valueOf(txn.getTrxnId()), // DB-generated txnID
                                        txn.getImporter(),
                                        txn.getExporter(),
                                        txn.getImporterBank(),
                                        txn.getExporterBank(),
                                        txn.getShipper(),
                                        txn.getGoods());

                        return ResponseEntity.status(HttpStatus.ACCEPTED).build();

                } catch (Exception e) {
                        // Revert to FAILED on error
                        txn.getBlToken().setTokenStatus(TokenStatus.FAILED);
                        transactionRepository.save(txn);

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Failed to issue BL. Please try again later.");
                }
        }
}
