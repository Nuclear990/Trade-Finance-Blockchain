package com.tradeAnchor.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
@Component
public class JwtUtil {
    private String keyString = "-i--want--another--internship--very--fast-";
    private SecretKey key = Keys.hmacShaKeyFor(keyString.getBytes());

    public String generateAccessToken(String username, String role){
        long expiration_time = 1000 * 60 * 5; // 5 mins
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration_time))
                .claim("type", "access")
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String accessToken){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
    }

    public String extractUsername(String accessToken){
        return extractAllClaims(accessToken).getSubject();
    }

    public String extractRole(String accessToken){
        return extractAllClaims(accessToken).get("role").toString();
    }
}
