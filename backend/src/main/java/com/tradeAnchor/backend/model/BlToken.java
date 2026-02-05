package com.tradeAnchor.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Entity
@Table(
        name = "bl_tokens",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "on_chain_bl_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BlToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blId;

    @Column(name = "on_chain_bl_id")
    private BigInteger onChainBlId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus tokenStatus;

    public BlToken(BigInteger onChainBlId, TokenStatus tokenStatus) {
        this.onChainBlId = onChainBlId;
        this.tokenStatus = tokenStatus;
    }
}
