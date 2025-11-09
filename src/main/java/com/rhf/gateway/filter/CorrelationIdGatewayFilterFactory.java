package com.rhf.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component("CorrelationId")
public class CorrelationIdGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {

        return (exchange, chain) -> {

            // 1. Read or create correlation ID
            String cid = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
            if (cid == null || cid.isEmpty()) {
                cid = UUID.randomUUID().toString();
            }

            // 2. Always set header on *response immediately* (for error cases)
            exchange.getResponse().getHeaders().set("X-Correlation-Id", cid);

            // 3. Mutate request to propagate to downstream services
            var mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest()
                            .mutate()
                            .header("X-Correlation-Id", cid)
                            .build())
                    .build();

            // 4. Ensure it's set for successful responses
            String finalCid = cid;
            return chain.filter(mutatedExchange)
                    .doOnError(err -> {
                        // error path - header already set
                    })
                    .then(Mono.fromRunnable(() -> {
                        // success path - ensure header is still present
                        mutatedExchange.getResponse()
                                .getHeaders()
                                .set("X-Correlation-Id", finalCid);
                    }));
        };
    }
}
