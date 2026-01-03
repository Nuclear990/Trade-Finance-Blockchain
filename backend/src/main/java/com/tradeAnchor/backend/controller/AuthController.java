package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.dto.LoginResponseDto;
import com.tradeAnchor.backend.dto.UserDto;
import com.tradeAnchor.backend.model.RefreshToken;
import com.tradeAnchor.backend.model.Users;
import com.tradeAnchor.backend.repository.RefreshTokenRepository;
import com.tradeAnchor.backend.repository.UsersRepository;
import com.tradeAnchor.backend.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

// 401 Unauthorized
// Access token missing or expired
// Refresh token missing or expired
// Authentication is required but not possible

// 403 Forbidden
// Credentials are wrong
// Token is malformed, forged, revoked, or valid-but-not-allowed

@RestController
@RequestMapping("/public")
public class AuthController {
    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    UsersRepository usersRepository;
    RefreshTokenRepository refreshTokenRepository;
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UsersRepository usersRepository, RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usersRepository = usersRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody UserDto u) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    u.getUsername(),
                                    u.getPassword()
                            )
                    );

            Users user = (Users) authentication.getPrincipal();
            //Refresh token - opaque string
            String refreshTokenValue = UUID.randomUUID().toString();
            refreshTokenRepository.save(
                    new RefreshToken(user.getUsername(), refreshTokenValue, user.getUserType())
            );
            // send refresh token as cookie
            ResponseCookie cookie = ResponseCookie.from("RefreshToken", refreshTokenValue)
                    .httpOnly(true)
                    .secure(false) // true in prod
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(60L * 60 * 24 * 30)
                    .build();

            LoginResponseDto dto = new LoginResponseDto(
                    jwtUtil.generateAccessToken(user.getUsername(), user.getUserType()),
                    user.getUserType()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(dto);

        } catch (BadCredentialsException ex) {
            // WRONG username/password → FORBIDDEN
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid credentials");
        }
    }


    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(
            @CookieValue(name = "RefreshToken", required = false) String token
    ) {
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing refresh token");
        }

        RefreshToken tokenEntity =
                refreshTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid refresh token")
                        );
// expired token - normal case
        if (tokenEntity.getExp().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token expired");
        }
// token reuse - security issue
        if (tokenEntity.getRevoked()) {
            refreshTokenRepository.revokeAllForUsername(tokenEntity.getUsername());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token reuse detected");
        }

        // rotate refresh token
        tokenEntity.setRevoked(true);
        tokenEntity.setExp(Instant.now());
        refreshTokenRepository.save(tokenEntity);

        String newRefreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.save(
                new RefreshToken(tokenEntity.getUsername(), newRefreshToken, tokenEntity.getUserType())
        );

        ResponseCookie cookie = ResponseCookie.from("RefreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(false) // true in prod
                .sameSite("Lax")
                .path("/")
                .maxAge(60L * 60 * 24 * 30)
                .build();

        LoginResponseDto dto = new LoginResponseDto(
                jwtUtil.generateAccessToken(
                        tokenEntity.getUsername(),
                        tokenEntity.getUserType()
                ),
                tokenEntity.getUserType()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(dto);
    }


    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("RefreshToken") String rt){
        refreshTokenRepository.findByToken(rt).ifPresent(tokenEntity -> refreshTokenRepository.revokeAndExpireAllForUsername(tokenEntity.getUsername(), Instant.now().plusSeconds(60L * 60 * 24 * 7)));
        ResponseCookie deleteCookie = ResponseCookie.from("RefreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }
}
