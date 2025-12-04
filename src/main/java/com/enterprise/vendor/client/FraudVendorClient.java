package com.enterprise.vendor.client;

import com.enterprise.vendor.dto.vendor.FraudRequest;
import com.enterprise.vendor.dto.vendor.FraudResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Client for Fraud Detection Vendor API integration.
 * 
 * @author Shivam Srivastav
 */
@Component
@Slf4j
public class FraudVendorClient {

    private final WebClient webClient;

    public FraudVendorClient(
            WebClient.Builder webClientBuilder,
            @Value("${vendors.fraud.base-url}") String baseUrl,
            @Value("${vendors.fraud.api-key}") String apiKey,
            @Value("${vendors.fraud.timeout-ms}") long timeoutMs) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    @CircuitBreaker(name = "fraudVendor", fallbackMethod = "fallbackCheckFraud")
    @Retry(name = "fraudVendor")
    public FraudResponse checkFraud(FraudRequest request) {
        log.info("Checking fraud for order: {}", request.getOrderId());

        try {
            return webClient.post()
                    .uri("/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FraudResponse.class)
                    .timeout(Duration.ofSeconds(4))
                    .block();
        } catch (Exception e) {
            log.error("Fraud vendor error: {}", e.getMessage());
            throw new RuntimeException("Fraud vendor unavailable", e);
        }
    }

    public FraudResponse fallbackCheckFraud(FraudRequest request, Exception e) {
        log.warn("Fraud vendor circuit open, approving with caution");

        // Conservative fallback - approve but flag for manual review
        return FraudResponse.builder()
                .score(0.5) // Medium risk
                .riskLevel("MEDIUM")
                .recommendation("REVIEW")
                .checkId("FALLBACK-" + request.getOrderId())
                .build();
    }
}
