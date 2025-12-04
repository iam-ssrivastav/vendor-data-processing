package com.enterprise.vendor.client;

import com.enterprise.vendor.dto.vendor.TaxRequest;
import com.enterprise.vendor.dto.vendor.TaxResponse;
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
 * Client for Tax Calculation Vendor API integration.
 * 
 * @author Shivam Srivastav
 */
@Component
@Slf4j
public class TaxVendorClient {

    private final WebClient webClient;

    public TaxVendorClient(
            WebClient.Builder webClientBuilder,
            @Value("${vendors.tax.base-url}") String baseUrl,
            @Value("${vendors.tax.api-key}") String apiKey,
            @Value("${vendors.tax.timeout-ms}") long timeoutMs) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    @CircuitBreaker(name = "taxVendor", fallbackMethod = "fallbackCalculateTax")
    @Retry(name = "taxVendor")
    public TaxResponse calculateTax(TaxRequest request) {
        log.info("Calculating tax for order: {}", request.getOrderId());

        try {
            return webClient.post()
                    .uri("/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TaxResponse.class)
                    .timeout(Duration.ofSeconds(2))
                    .block();
        } catch (Exception e) {
            log.error("Tax vendor error: {}", e.getMessage());
            throw new RuntimeException("Tax vendor unavailable", e);
        }
    }

    public TaxResponse fallbackCalculateTax(TaxRequest request, Exception e) {
        log.warn("Tax vendor circuit open, using default tax rate");

        // Use default 8% tax rate
        BigDecimal taxAmount = request.getAmount().multiply(new BigDecimal("0.08"));

        return TaxResponse.builder()
                .taxAmount(taxAmount)
                .taxRate(new BigDecimal("0.08"))
                .jurisdiction("DEFAULT")
                .build();
    }
}
