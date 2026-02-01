package com.tradeAnchor.backend.filter;

import com.tradeAnchor.backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if(request.getServletPath().startsWith("/secure") && authHeader == null){
            System.out.println("\n\n\nAUTH HEADER NULL\n\n\n");
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

            // token type mismatch → FORBIDDEN
            if (!"access".equals(claims.get("type"))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            String username = claims.getSubject();
            String role = claims.get("userType", String.class);

            var authority = new SimpleGrantedAuthority(role);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(authority)));

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // expired access token → UNAUTHORIZED (frontend may refresh)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("TokenExpired", "true");
        } catch (Exception e) {
            // malformed / forged / invalid signature → FORBIDDEN
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
