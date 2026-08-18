package org.enterpriseauditing.enterpriseauditing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enterpriseauditing.enterpriseauditing.model.Role;
import org.enterpriseauditing.enterpriseauditing.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        // No Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7);

        // Invalid JWT
        try {

            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                String username =
                        jwtService.extractUsername(token);

                String role =
                        jwtService.extractRole(token);

                Role userRole;

                try {
                    userRole = Role.valueOf(role);
                } catch (IllegalArgumentException e) {
                    filterChain.doFilter(request, response);
                    return;
                }

                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(
                                "ROLE_" + userRole.name()
                        );
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(authority)
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception e) {

            SecurityContextHolder
                    .clearContext();
        }

        filterChain.doFilter(request, response);
    }
}