package com.enterprise.vendor.simulator;

import com.enterprise.vendor.dto.vendor.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Mock Vendor API Simulator.
 * 
 * Simulates external vendor APIs for testing:
 * - Payment processing
 * - Shipping rate calculation
 * - Tax calculation
 * - Fraud detection
 * 
 * Enable with: vendor.simulator.enabled=true
 * 
 * @author Shivam Srivastav
 */
@RestController
@RequestMapping("/mock-vendor")
@Slf4j
@ConditionalOnProperty(name = "vendor.simulator.enabled", havingValue = "true", matchIfMissing = true)
public class MockVendorSimulator {

    private final Random random = new Random();

    /**
     * Mock Payment API - Simulates Stripe/PayPal
     */
    @PostMapping("/payment-api/charge")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Mock Payment API: Processing payment for order {}", request.getOrderId());

        // Simulate 80% success rate
        boolean success = random.nextDouble() < 0.8;

        PaymentResponse response = PaymentResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .status(success ? "SUCCESS" : "FAILED")
                .message(success ? "Payment processed successfully" : "Insufficient funds")
                .timestamp(System.currentTimeMillis())
                .build();

        log.info("Mock Payment API: Order {} - Status: {}", request.getOrderId(), response.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Mock Shipping API - Simulates FedEx/UPS
     */
    @PostMapping("/shipping-api/rates")
    public ResponseEntity<ShippingResponse> calculateShipping(@RequestBody ShippingRequest request) {
        log.info("Mock Shipping API: Calculating rate for order {}", request.getOrderId());

        // Calculate based on service type
        BigDecimal cost = switch (request.getServiceType()) {
            case "EXPRESS" -> new BigDecimal("24.99");
            case "OVERNIGHT" -> new BigDecimal("39.99");
            default -> new BigDecimal("9.99");
        };

        ShippingResponse response = ShippingResponse.builder()
                .cost(cost)
                .trackingNumber("TRACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .estimatedDays("EXPRESS".equals(request.getServiceType()) ? 2 : 5)
                .carrier("MOCK_CARRIER")
                .build();

        log.info("Mock Shipping API: Order {} - Cost: ${}", request.getOrderId(), cost);
        return ResponseEntity.ok(response);
    }

    /**
     * Mock Tax API - Simulates Avalara
     */
    @PostMapping("/tax-api/calculate")
    public ResponseEntity<TaxResponse> calculateTax(@RequestBody TaxRequest request) {
        log.info("Mock Tax API: Calculating tax for order {}", request.getOrderId());

        // Tax rate based on state
        BigDecimal taxRate = getTaxRateByState(request.getAddress().getState());
        BigDecimal taxAmount = request.getAmount().multiply(taxRate);

        TaxResponse response = TaxResponse.builder()
                .taxAmount(taxAmount)
                .taxRate(taxRate)
                .jurisdiction(request.getAddress().getState())
                .build();

        log.info("Mock Tax API: Order {} - Tax: ${} ({}%)",
                request.getOrderId(), taxAmount, taxRate.multiply(new BigDecimal("100")));
        return ResponseEntity.ok(response);
    }

    /**
     * Mock Fraud API - Simulates fraud detection service
     */
    @PostMapping("/fraud-api/check")
    public ResponseEntity<FraudResponse> checkFraud(@RequestBody FraudRequest request) {
        log.info("Mock Fraud API: Checking fraud for order {}", request.getOrderId());

        // Generate random fraud score (0.0 - 1.0)
        double score = random.nextDouble();
        String riskLevel;
        String recommendation;

        if (score < 0.3) {
            riskLevel = "LOW";
            recommendation = "APPROVE";
        } else if (score < 0.7) {
            riskLevel = "MEDIUM";
            recommendation = "REVIEW";
        } else {
            riskLevel = "HIGH";
            recommendation = "DECLINE";
        }

        FraudResponse response = FraudResponse.builder()
                .score(score)
                .riskLevel(riskLevel)
                .recommendation(recommendation)
                .checkId("FRAUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        log.info("Mock Fraud API: Order {} - Score: {}, Risk: {}, Recommendation: {}",
                request.getOrderId(), String.format("%.2f", score), riskLevel, recommendation);
        return ResponseEntity.ok(response);
    }

    private BigDecimal getTaxRateByState(String state) {
        return switch (state) {
            case "CA" -> new BigDecimal("0.0725"); // California
            case "NY" -> new BigDecimal("0.0800"); // New York
            case "TX" -> new BigDecimal("0.0625"); // Texas
            case "FL" -> new BigDecimal("0.0600"); // Florida
            default -> new BigDecimal("0.0700"); // Default
        };
    }
}
