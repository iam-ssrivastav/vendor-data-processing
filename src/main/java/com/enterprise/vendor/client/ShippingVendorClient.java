package com.enterprise.vendor.client;

import com.enterprise.vendor.dto.vendor.ShippingRequest;
import com.enterprise.vendor.dto.vendor.ShippingResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Client for Shipping Vendor API integration.
 * 
 * @author Shivam Srivastav
 */
@Component
@Slf4j
public class ShippingVendorClient {

    private final WebClient webClient;

    public ShippingVendorClient(
            WebClient.Builder webClientBuilder,
            @Value("${vendors.shipping.base-url}") String baseUrl,
            @Value("${vendors.shipping.api-key}") String apiKey,
            @Value("${vendors.shipping.timeout-ms}") long timeoutMs) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    @CircuitBreaker(name = "shippingVendor", fallbackMethod = "fallbackCalculateRate")
    @Retry(name = "shippingVendor")
    public ShippingResponse calculateShippingRate(ShippingRequest request) {
        log.info("Calculating shipping rate for order: {}", request.getOrderId());

        try {
            return webClient.post()
                    .uri("/rates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ShippingResponse.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();
        } catch (Exception e) {
            log.error("Shipping vendor error: {}", e.getMessage());
            throw new RuntimeException("Shipping vendor unavailable", e);
        }
    }

    public ShippingResponse fallbackCalculateRate(ShippingRequest request, Exception e) {
        log.warn("Shipping vendor circuit open, using default rate");

        // Return default shipping cost
        return ShippingResponse.builder()
                .cost(new BigDecimal("9.99"))
                .trackingNumber("PENDING")
                .estimatedDays(5)
                .carrier("STANDARD")
                .build();
    }
}
