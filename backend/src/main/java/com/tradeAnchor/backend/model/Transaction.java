package com.tradeAnchor.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trxnId;

    @OneToOne
    @JoinColumn(
            name = "bl_token_id",
            foreignKey = @ForeignKey(name = "fk_transaction_bl_token")
    )
    private BlToken blToken;

    @OneToOne
    @JoinColumn(
            name = "lc_token_id",
            foreignKey = @ForeignKey(name = "fk_transaction_lc_token")
    )
    private LcToken lcToken;

    @NotBlank
    @Column(nullable = false)
    private String importer;

    @NotBlank
    @Column(nullable = false)
    private String exporter;

    @NotBlank
    @Column(nullable = false)
    private String shipper;

    @NotBlank
    @Column(nullable = false)
    private String importerBank;

    @NotBlank
    @Column(nullable = false)
    private String exporterBank;

    @NotNull
    @Positive
    @Column(nullable = false)
    private BigInteger amount;

    @NotNull
    @Positive
    @Column(nullable = false)
    private BigInteger goods;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrxnStatus trxnStatus;

    public Transaction(
            BlToken blToken,
            LcToken lcToken,
            String importer,
            String exporter,
            String shipper,
            String importerBank,
            String exporterBank,
            BigInteger amount,
            BigInteger goods,
            TrxnStatus trxnStatus
    ) {
        this.blToken = blToken;
        this.lcToken = lcToken;
        this.importer = importer;
        this.exporter = exporter;
        this.shipper = shipper;
        this.importerBank = importerBank;
        this.exporterBank = exporterBank;
        this.amount = amount;
        this.goods = goods;
        this.trxnStatus = trxnStatus;
    }

}
