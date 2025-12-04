package com.enterprise.vendor.client;

import com.enterprise.vendor.dto.vendor.PaymentRequest;
import com.enterprise.vendor.dto.vendor.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for Payment Vendor API integration.
 * 
 * Demonstrates:
 * - Circuit breaker pattern for fault tolerance
 * - Retry mechanism with exponential backoff
 * - Async HTTP calls with WebClient
 * - Webhook-based async processing
 * 
 * @author Shivam Srivastav
 */
@Component
@Slf4j
public class PaymentVendorClient {

    private final WebClient webClient;
    private final String apiKey;

    public PaymentVendorClient(
            WebClient.Builder webClientBuilder,
            @Value("${vendors.payment.base-url}") String baseUrl,
            @Value("${vendors.payment.api-key}") String apiKey,
            @Value("${vendors.payment.timeout-ms}") long timeoutMs) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
        this.apiKey = apiKey;
    }

    /**
     * Process payment with external vendor.
     * 
     * Circuit breaker opens after 50% failure rate in sliding window of 10
     * requests.
     * Retries up to 3 times with exponential backoff (1s, 2s, 4s).
     */
    @CircuitBreaker(name = "paymentVendor", fallbackMethod = "fallbackProcessPayment")
    @Retry(name = "paymentVendor")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Calling payment vendor for order: {}", request.getOrderId());

        try {
            return webClient.post()
                    .uri("/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.error("Payment vendor error for order {}: {}", request.getOrderId(), e.getMessage());
            throw new RuntimeException("Payment vendor unavailable", e);
        }
    }

    /**
     * Fallback method when circuit is open or all retries exhausted.
     */
    public PaymentResponse fallbackProcessPayment(PaymentRequest request, Exception e) {
        log.warn("Payment vendor circuit open for order: {}. Reason: {}",
                request.getOrderId(), e.getMessage());

        // Return pending status - will be retried via Kafka
        return PaymentResponse.builder()
                .transactionId("PENDING-" + request.getOrderId())
                .status("PENDING")
                .message("Payment queued for retry - vendor temporarily unavailable")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
