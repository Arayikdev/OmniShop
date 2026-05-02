package com.omnishop.apigateway.config;

import com.omnishop.apigateway.filter.JwtAuthFilter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtAuthFilterConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter();
    }

    @Bean
    public JwtAuthFilterGatewayFilterFactory jwtAuthFilterGatewayFilterFactory(JwtAuthFilter jwtAuthFilter) {
        return new JwtAuthFilterGatewayFilterFactory(jwtAuthFilter);
    }

    public static class JwtAuthFilterGatewayFilterFactory
            extends AbstractGatewayFilterFactory<Object> {

        private final JwtAuthFilter jwtAuthFilter;

        public JwtAuthFilterGatewayFilterFactory(JwtAuthFilter jwtAuthFilter) {
            super(Object.class);
            this.jwtAuthFilter = jwtAuthFilter;
        }

        @Override
        public GatewayFilter apply(Object config) {
            return jwtAuthFilter;
        }
    }
}
