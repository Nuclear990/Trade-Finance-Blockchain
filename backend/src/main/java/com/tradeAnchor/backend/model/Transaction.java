package com.tradeAnchor.backend.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Reference;

@NoArgsConstructor
@Getter
@Setter
public class Transactions {
    Long TrxnId;
    Long OnChainTrxnId;
    @Reference
    Long BlTokenId;
    @Reference
    Long LcTokenId;
    String Importer;
    String Exporter;
    String Shipper;
    String ImporterBank;
    String ExporterBank;
    Long amount;
    Long goods;
}
