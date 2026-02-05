package com.tradeAnchor.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {

    @NotBlank(message = "Company must be provided")
    private String company;

    @NotBlank(message = "Importer Bank must be provided")
    private String importerBank;

    @NotBlank(message = "Exporter Bank must be provided")
    private String exporterBank;

    @NotBlank(message = "Shipper must be provided")
    private String shipper;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigInteger amount;

    @NotNull(message = "Goods quantity is required")
    @Positive(message = "Goods must be greater than zero")
    private BigInteger goods;
}
