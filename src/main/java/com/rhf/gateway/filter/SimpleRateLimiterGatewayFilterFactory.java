package com.rhf.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("SimpleRateLimiter")
public class SimpleRateLimiterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<SimpleRateLimiterGatewayFilterFactory.Config> {

    public SimpleRateLimiterGatewayFilterFactory() {
        super(Config.class);
    }

    private static final Map<String, Bucket> BUCKETS = new ConcurrentHashMap<>();

    @Override
    public GatewayFilter apply(Config c) {
        return (exchange, chain) -> {
            ServerHttpRequest req = exchange.getRequest();

            String key = req.getHeaders().getFirst(c.keyHeader);
            if (key == null) {
                InetSocketAddress remote = req.getRemoteAddress();
                key = remote != null ? remote.getAddress().getHostAddress() : "anonymous";
            }

            Bucket bucket = BUCKETS.computeIfAbsent(key,
                    k -> new Bucket(c.burstCapacity, c.replenishPerSecond));

            if (bucket.tryConsume()) {
                exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(bucket.remaining()));
                return chain.filter(exchange);
            }

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        };
    }

    public static class Config {
        public int replenishPerSecond = 5;
        public int burstCapacity = 10;
        public String keyHeader = "X-Api-Key";
    }

    static class Bucket {
        private final int capacity;
        private final int refillRate;
        private double tokens;
        private long lastRefill = System.nanoTime();

        Bucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
        }

        synchronized void refill() {
            long now = System.nanoTime();
            double seconds = (now - lastRefill) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + seconds * refillRate);
            lastRefill = now;
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        synchronized int remaining() {
            refill();
            return (int) tokens;
        }
    }
}
