package com.marketplace.trade.config;

import org.slf4j.MDC;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

@Component
public class RestTemplateInterceptor {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    public ClientHttpRequestInterceptor correlationInterceptor() {
        return (request, body, execution) -> {
            String correlationId = MDC.get(MDC_KEY);
            if (correlationId != null) {
                request.getHeaders().add(CORRELATION_HEADER, correlationId);
            }
            return execution.execute(request, body);
        };
    }
}
