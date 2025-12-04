package com.enterprise.vendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for Vendor Data Processing System.
 * 
 * This system demonstrates integration with multiple external vendors:
 * - Payment processing (Stripe-like)
 * - Shipping rate calculation (FedEx/UPS-like)
 * - Tax calculation (Avalara-like)
 * - Fraud detection (real-time scoring)
 * 
 * Key Features:
 * - Event-driven architecture with Kafka
 * - Circuit breaker and retry patterns
 * - Webhook handling for async vendor responses
 * - Comprehensive observability
 * 
 * @author Shivam Srivastav
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
public class VendorDataProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(VendorDataProcessingApplication.class, args);
    }
}
