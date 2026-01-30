package com.tradeAnchor.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String username;
    @Column(nullable = false, unique = true)
    private String token;
    private Boolean revoked;
    private Instant iat; //issue at
    private Instant exp;   // initially - natural exp date. Once used - date at which retention period ends, for cleaner cron job.
    @Enumerated(EnumType.STRING)
    private UserType userType;

    public RefreshToken(String username, String token, UserType userType){
        this.username = username;
        this.token = token;
        this.revoked = false;
        this.iat = Instant.now();
        this.exp = Instant.now().plusSeconds(60L*60*24*30);  //one month
        this.userType = userType;
    }
}
