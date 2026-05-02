package com.omnishop.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class JwtAuthFilter implements GatewayFilter, Ordered {

    @Value("${app.jwt.secret}")
    private String secret;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerWebExchange stripped = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                }))
                .build();

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(stripped, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            return unauthorizedResponse(stripped, "Empty token");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
                return unauthorizedResponse(stripped, "Invalid token claims");
            }

            ServerHttpRequest mutatedRequest = stripped.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(stripped.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            return unauthorizedResponse(stripped, "Token expired");
        } catch (JwtException e) {
            return unauthorizedResponse(stripped, "Invalid token");
        } catch (Exception e) {
            return unauthorizedResponse(stripped, "Authentication failed");
        }
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
