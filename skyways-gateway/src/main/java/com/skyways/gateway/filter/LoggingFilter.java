package com.skyways.gateway.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public class LoggingFilter implements GatewayFilter {

    private static final Logger log = LogManager.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders()
            .getFirst("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalTraceId = traceId;
        final long startMs = System.currentTimeMillis();

        ServerWebExchange mutated = exchange.mutate()
            .request(r -> r.header("X-Trace-Id", finalTraceId))
            .build();

        // Add trace ID to response headers before the response is committed
        mutated.getResponse().getHeaders().add("X-Trace-Id", finalTraceId);

        log.info("[traceId={}] --> {} {}",
            finalTraceId,
            mutated.getRequest().getMethod(),
            mutated.getRequest().getURI().getPath());

        return chain.filter(mutated).doFinally(signal ->
            log.info("[traceId={}] <-- {} {} | {}ms | signal={}",
                finalTraceId,
                mutated.getRequest().getMethod(),
                mutated.getRequest().getURI().getPath(),
                System.currentTimeMillis() - startMs,
                signal)
        );
    }
}
