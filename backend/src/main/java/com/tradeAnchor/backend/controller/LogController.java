package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.dto.LogDto;
import com.tradeAnchor.backend.model.Transaction;
import com.tradeAnchor.backend.model.Users;
import com.tradeAnchor.backend.repository.TransactionRepository;
import com.tradeAnchor.backend.repository.UsersRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class LogController {

    private final TransactionRepository transactionRepository;
    private final UsersRepository usersRepository;

    public LogController(
            TransactionRepository transactionRepository,
            UsersRepository usersRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.usersRepository = usersRepository;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogDto>> logs() {

        Users user = (Users) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Set<Long> txnIds = user.getTrxns();

        if (txnIds == null || txnIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // 1. Load transactions in one query
        List<Transaction> transactions =
                transactionRepository.findAllById(txnIds);

        // 2. Collect ALL ethereum addresses referenced in logs
        Set<String> addresses = transactions.stream()
                .flatMap(txn -> Stream.of(
                        txn.getImporter(),
                        txn.getExporter(),
                        txn.getImporterBank(),
                        txn.getExporterBank(),
                        txn.getShipper()
                ))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. Resolve addresses → usernames in ONE query
        Map<String, String> addressToUsername =
                usersRepository.findByEthereumAddressIn(addresses)
                        .stream()
                        .collect(Collectors.toMap(
                                Users::getEthereumAddress,
                                Users::getUsername
                        ));

        // 4. Map to DTO
        List<LogDto> logs = transactions.stream()
                .map(txn -> new LogDto(
                        txn.getTrxnId(),

                        txn.getLcToken() != null
                                ? txn.getLcToken().getTokenStatus()
                                : null,

                        txn.getBlToken() != null
                                ? txn.getBlToken().getTokenStatus()
                                : null,

                        addressToUsername.get(txn.getImporter()),
                        addressToUsername.get(txn.getExporter()),
                        addressToUsername.get(txn.getImporterBank()),
                        addressToUsername.get(txn.getExporterBank()),
                        addressToUsername.get(txn.getShipper()),

                        txn.getAmount(),
                        txn.getGoods()
                ))
                .toList();

        return ResponseEntity.ok(logs);
    }
}
