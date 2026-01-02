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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

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
    public ResponseEntity<LoginResponseDto> login(@RequestBody UserDto u){

        System.out.println("### LOGIN ATTEMPT username=" + u.getUsername());

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                u.getUsername(),
                                u.getPassword()
                        )
                );

        Users user = (Users) authentication.getPrincipal();

        //generate refresh token - opaque string
        String rToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(user.getUsername(), rToken, user.getUserType());
        refreshTokenRepository.save(refreshToken);
        //create refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", rToken)
                .httpOnly(true)
                .secure(false)        // mandatory in production (HTTPS only)
                .sameSite("Lax")
                .path("/") // critical for browser to send it back later
                .maxAge(60L*60*24*30)
                .build();
        LoginResponseDto responseDto = new LoginResponseDto(jwtUtil.generateAccessToken(user.getUsername(), user.getUserType()), user.getUserType());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(responseDto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@CookieValue("RefreshToken") String token){
        //invalidate old refresh token
        RefreshToken tokenEntity =
                refreshTokenRepository.findByToken(token).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"
                ));

// expired: normal failure
        if (tokenEntity.getExp().isBefore(Instant.now())) {
            return ResponseEntity.status(401).build();
        }

// revoked: replay attack
        if (tokenEntity.getRevoked()) {
            // revoke all tokens for this user
            refreshTokenRepository.revokeAllForUsername(tokenEntity.getUsername());

            // delete cookie
            ResponseCookie deleteCookie = ResponseCookie.from("RefreshToken", "")
                    .path("/")
                    .httpOnly(true)
                    .secure(false)
                    .maxAge(0)
                    .build();

            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .build();
        }

        tokenEntity.setRevoked(true);
        tokenEntity.setExp(Instant.now()); // 1 week
        refreshTokenRepository.save(tokenEntity);  // updated repository
        //generate new refresh token - opaque string
        String rToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(tokenEntity.getUsername(), rToken, tokenEntity.getUserType());
        refreshTokenRepository.save(refreshToken);
        //send refresh token as cookie
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", rToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/") // critical for browser to send it back later
                .maxAge(60L*60*24*30)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(jwtUtil.generateAccessToken(tokenEntity.getUsername(), tokenEntity.getUserType()));
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
