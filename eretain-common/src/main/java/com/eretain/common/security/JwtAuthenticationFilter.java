package com.eretain.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                List<String> roles = jwtUtil.extractRoles(token);
                Long userId = jwtUtil.extractUserId(token);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                UserPrincipal principal = new UserPrincipal(userId, username, roles);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else {
            // Support inter-service calls via gateway-forwarded headers (X-User-Id, X-User-Roles)
            String userIdHeader = request.getHeader("X-User-Id");
            String rolesHeader = request.getHeader("X-User-Roles");
            String usernameHeader = request.getHeader("X-User-Name");

            if (userIdHeader != null && rolesHeader != null) {
                try {
                    Long userId = Long.valueOf(userIdHeader);
                    String username = usernameHeader != null ? usernameHeader : "service-user";
                    List<String> roles = List.of(rolesHeader.split(","));

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> role.startsWith("ROLE_") ? new SimpleGrantedAuthority(role)
                                    : new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    UserPrincipal principal = new UserPrincipal(userId, username, roles);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (NumberFormatException e) {
                    // Invalid header, skip authentication
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
