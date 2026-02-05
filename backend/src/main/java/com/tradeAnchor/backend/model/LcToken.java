package com.tradeAnchor.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Entity
@Table(
        name = "lc_tokens",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "on_chain_lc_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LcToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lcId;

    @Column(name = "on_chain_lc_id")
    private BigInteger onChainLcId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus tokenStatus;

    public LcToken(BigInteger onChainLcId, TokenStatus tokenStatus) {
        this.onChainLcId = onChainLcId;
        this.tokenStatus = tokenStatus;
    }
}
