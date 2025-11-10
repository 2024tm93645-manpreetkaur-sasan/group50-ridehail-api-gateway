package com.rhf.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component("CorrelationId")
public class CorrelationIdGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    public GatewayFilter apply(Object config) {

        return (exchange, chain) -> {

            // 1. Read or create ID
            String cid = exchange.getRequest().getHeaders().getFirst(HEADER);
            if (cid == null || cid.isBlank()) {
                cid = UUID.randomUUID().toString();
            }

            // 2. Add correlation ID to MDC (for logs)
            MDC.put(MDC_KEY, cid);

            // 3. Add header early—even for errors
            exchange.getResponse().getHeaders().set(HEADER, cid);

            // 4. Propagate header to downstream
            var mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest()
                            .mutate()
                            .header(HEADER, cid)
                            .build())
                    .build();

            String finalCid = cid;

            return chain.filter(mutatedExchange)
                    // ensure response always contains the correlation id
                    .then(Mono.fromRunnable(() ->
                            mutatedExchange.getResponse()
                                    .getHeaders()
                                    .set(HEADER, finalCid)
                    ))
                    // Cleanup MDC after request completes (success, error, cancel)
                    .doFinally(signalType -> MDC.remove(MDC_KEY)).then();
        };
    }
}
