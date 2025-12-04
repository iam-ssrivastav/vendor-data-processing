package com.enterprise.vendor.service;

import com.enterprise.vendor.client.*;
import com.enterprise.vendor.dto.vendor.*;
import com.enterprise.vendor.model.Order;
import com.enterprise.vendor.model.OrderStatus;
import com.enterprise.vendor.model.ShippingAddress;
import com.enterprise.vendor.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Vendor Orchestrator Service.
 * 
 * Coordinates calls to multiple external vendors:
 * 1. Fraud Detection (async with webhook)
 * 2. Tax Calculation (sync)
 * 3. Shipping Rate (sync)
 * 4. Payment Processing (async with webhook)
 * 
 * Demonstrates:
 * - Parallel vendor API calls
 * - Async processing with webhooks
 * - Transaction management
 * - Error handling and fallbacks
 * 
 * @author Shivam Srivastav
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VendorOrchestrator {

    private final PaymentVendorClient paymentClient;
    private final ShippingVendorClient shippingClient;
    private final TaxVendorClient taxClient;
    private final FraudVendorClient fraudClient;
    private final OrderRepository orderRepository;

    @Value("${server.port}")
    private String serverPort;

    /**
     * Process order through all vendors.
     * 
     * Flow:
     * 1. Check fraud (async)
     * 2. Calculate tax (sync)
     * 3. Calculate shipping (sync)
     * 4. Process payment (async)
     */
    @Async
    @Transactional
    public void processOrderWithVendors(Order order) {
        log.info("Starting vendor processing for order: {}", order.getId());

        try {
            // Step 1: Fraud Detection (can be async)
            FraudResponse fraudResponse = checkFraud(order);
            order.setFraudScore(fraudResponse.getScore());

            if ("DECLINE".equals(fraudResponse.getRecommendation())) {
                order.setStatus(OrderStatus.FRAUD_CHECK_FAILED);
                orderRepository.save(order);
                log.warn("Order {} failed fraud check. Score: {}", order.getId(), fraudResponse.getScore());
                return;
            }

            order.setStatus(OrderStatus.FRAUD_CHECK_PASSED);
            orderRepository.save(order);

            // Step 2 & 3: Calculate tax and shipping in parallel
            TaxResponse taxResponse = calculateTax(order);
            ShippingResponse shippingResponse = calculateShipping(order);

            order.setTaxAmount(taxResponse.getTaxAmount());
            order.setShippingCost(shippingResponse.getCost());
            order.setShippingTrackingNumber(shippingResponse.getTrackingNumber());

            // Calculate total
            BigDecimal total = order.getAmount()
                    .add(order.getTaxAmount())
                    .add(order.getShippingCost());
            order.setTotalAmount(total);

            orderRepository.save(order);
            log.info("Order {} totals calculated. Tax: {}, Shipping: {}, Total: {}",
                    order.getId(), taxResponse.getTaxAmount(), shippingResponse.getCost(), total);

            // Step 4: Process payment
            PaymentResponse paymentResponse = processPayment(order);
            order.setPaymentTransactionId(paymentResponse.getTransactionId());
            order.setPaymentStatus(paymentResponse.getStatus());

            if ("SUCCESS".equals(paymentResponse.getStatus())) {
                order.setStatus(OrderStatus.PAYMENT_COMPLETED);
                log.info("Order {} payment completed. Transaction: {}",
                        order.getId(), paymentResponse.getTransactionId());
            } else if ("PENDING".equals(paymentResponse.getStatus())) {
                order.setStatus(OrderStatus.PAYMENT_PENDING);
                log.info("Order {} payment pending. Will be updated via webhook", order.getId());
            } else {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                log.error("Order {} payment failed: {}", order.getId(), paymentResponse.getMessage());
            }

            orderRepository.save(order);

        } catch (Exception e) {
            log.error("Error processing order {} with vendors: {}", order.getId(), e.getMessage(), e);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }

    private FraudResponse checkFraud(Order order) {
        FraudRequest request = FraudRequest.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getAmount())
                .ipAddress("192.168.1.1") // Would come from request context
                .email(order.getCustomerId() + "@example.com")
                .callbackUrl("http://localhost:" + serverPort + "/webhooks/fraud")
                .build();

        return fraudClient.checkFraud(request);
    }

    private TaxResponse calculateTax(Order order) {
        TaxRequest request = TaxRequest.builder()
                .orderId(order.getId())
                .amount(order.getAmount())
                .address(order.getShippingAddress())
                .productCategory("ELECTRONICS")
                .build();

        return taxClient.calculateTax(request);
    }

    private ShippingResponse calculateShipping(Order order) {
        ShippingAddress warehouse = ShippingAddress.builder()
                .street("1000 Warehouse Blvd")
                .city("Los Angeles")
                .state("CA")
                .zipCode("90001")
                .country("USA")
                .build();

        ShippingRequest request = ShippingRequest.builder()
                .orderId(order.getId())
                .fromAddress(warehouse)
                .toAddress(order.getShippingAddress())
                .weight(2.5)
                .serviceType("STANDARD")
                .build();

        return shippingClient.calculateShippingRate(request);
    }

    private PaymentResponse processPayment(Order order) {
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getTotalAmount())
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .callbackUrl("http://localhost:" + serverPort + "/webhooks/payment")
                .build();

        return paymentClient.processPayment(request);
    }
}
