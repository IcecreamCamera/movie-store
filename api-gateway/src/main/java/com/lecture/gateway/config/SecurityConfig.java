package com.lecture.gateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            // 루트는 프론트로 리다이렉트하는 라우트다. 인증을 걸면 리다이렉트 전에 401 이 난다.
            "/",
            "/oauth2/**",
            "/login",
            "/logout",
            "/error",
            "/favicon.ico",
            "/default-ui.css",
            "/actuator/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/users/register",
            "/api/users/login"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 서비스 간 내부 호출 전용 경로. 서비스끼리는 Eureka 서비스명으로 직접
                        // 호출하므로 게이트웨이를 거치지 않는다. 외부에서 들어올 이유가 없다.
                        .pathMatchers("/api/*/internal/**").denyAll()
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        // 영화 조회는 로그인 없이 열어 둔다. 예매 사이트에서 홈화면 영화 목록이
                        // 로그인을 요구하면 첫 화면이 비어 버린다. 쓰기(예매/결제)는 여전히 인증이 필요하다.
                        .pathMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * CORS 설정.
     *
     * CorsWebFilter 를 빈으로 노출하면 안 된다. 그것은 WebFilter 라 필터 체인에 자동 등록되는데,
     * http.cors() 도 CorsWebFilter 빈을 찾아 보안 체인 안에서 한 번 더 적용한다.
     * 결과적으로 Access-Control-Allow-Origin 이 두 번 붙어 브라우저가
     * "contains multiple values" 로 요청을 거부한다.
     *
     * CorsConfigurationSource 로 노출하면 http.cors() 가 이것만 사용해 한 번만 적용된다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
