package com.tradeAnchor.backend.dto;

import com.tradeAnchor.backend.model.TokenStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
public class LogDto {
    private Long trxnId;
    private TokenStatus lcToken;
    private TokenStatus blToken;
    private String importer;
    private String exporter;
    private String importerBank;
    private String exporterBank;
    private String shipper;
    private BigInteger amount;
    private BigInteger goods;
}
