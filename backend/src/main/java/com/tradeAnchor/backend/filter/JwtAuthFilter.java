package com.tradeAnchor.backend.filter;

import com.tradeAnchor.backend.model.Users;
import com.tradeAnchor.backend.repository.UsersRepository;
import com.tradeAnchor.backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UsersRepository usersRepository) {
        this.jwtUtil = jwtUtil;
        this.usersRepository = usersRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");


        // protect secured routes
        if ((request.getServletPath().startsWith("/bank") ||
                request.getServletPath().startsWith("/company")||
                request.getServletPath().startsWith("/shipper") ||
                request.getServletPath().startsWith("/log")) && authHeader == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.extractAllClaims(token);

            // token type check
            if (!"access".equals(claims.get("type"))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            String username = claims.getSubject();

            Users user = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            System.out.println("=== SECURITY DEBUG ===");
            System.out.println("Username        : " + user.getUsername());
            System.out.println("UserType (DB)   : " + user.getUserType());
            System.out.println("Authorities     : " + user.getAuthorities());
            System.out.println("======================");
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("TokenExpired", "true");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
