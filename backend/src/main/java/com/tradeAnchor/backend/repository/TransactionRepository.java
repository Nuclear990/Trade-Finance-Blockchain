package com.tradeAnchor.backend.repository;

import com.tradeAnchor.backend.model.Transaction;
import com.tradeAnchor.backend.model.TrxnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByImporter(String importer);

    List<Transaction> findByExporter(String exporter);

}
