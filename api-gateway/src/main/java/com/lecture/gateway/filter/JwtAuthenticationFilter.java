package com.lecture.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> mutateExchange(exchange, jwt))
                .flatMap(chain::filter)
                .switchIfEmpty(chain.filter(exchange));
    }

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, Jwt jwt) {
        String subject = jwt.getSubject();
        boolean subjectLooksLikeEmail = subject != null && subject.contains("@");

        String userId = jwt.getClaimAsString("user_id");
        if (isBlank(userId)) {
            userId = jwt.getClaimAsString("id");
        }
        if (isBlank(userId) && !subjectLooksLikeEmail) {
            userId = subject;
        }

        String email = jwt.getClaimAsString("email");
        if (isBlank(email) && subjectLooksLikeEmail) {
            email = subject;
        }

        String role = jwt.getClaimAsString("role");

        log.debug("JWT Filter - subject: {}, userId: {}, email: {}, role: {}", subject, userId, email, role);

        final String finalUserId = userId;
        final String finalEmail = email;
        final String finalRole = role;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    if (isBlank(finalUserId)) {
                        headers.remove("X-User-Id");
                    } else {
                        headers.set("X-User-Id", finalUserId);
                    }
                    if (isBlank(finalEmail)) {
                        headers.remove("X-User-Email");
                    } else {
                        headers.set("X-User-Email", finalEmail);
                    }
                    if (isBlank(finalRole)) {
                        headers.remove("X-User-Role");
                    } else {
                        headers.set("X-User-Role", finalRole);
                    }
                })
                .build();

        return exchange.mutate().request(mutatedRequest).build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
