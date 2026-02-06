package com.tradeAnchor.backend.repository;

import com.tradeAnchor.backend.model.TokenStatus;
import com.tradeAnchor.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByImporter(String importer);

    List<Transaction> findByExporter(String exporter);

    List<Transaction> findByImporterBankAndLcToken_TokenStatus(
            String importerBank,
            TokenStatus status);

    List<Transaction> findByLcToken_TokenStatus(TokenStatus status);

    List<Transaction> findByExporterBankAndLcToken_TokenStatusAndBlToken_TokenStatus(
            String exporterBank,
            TokenStatus lcStatus,
            TokenStatus blStatus);
}
