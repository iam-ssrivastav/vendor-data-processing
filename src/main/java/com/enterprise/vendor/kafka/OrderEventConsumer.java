package com.enterprise.vendor.kafka;

import com.enterprise.vendor.model.Order;
import com.enterprise.vendor.service.VendorOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events.
 * 
 * Consumes OrderCreatedEvents and triggers vendor processing.
 * 
 * @author Shivam Srivastav
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final VendorOrchestrator vendorOrchestrator;

    @KafkaListener(topics = "order-created-events", groupId = "vendor-processing-group")
    public void consumeOrderCreated(Order order) {
        log.info("Consumed OrderCreatedEvent for order: {}", order.getId());

        try {
            // Trigger async vendor processing
            vendorOrchestrator.processOrderWithVendors(order);
        } catch (Exception e) {
            log.error("Error processing order {}: {}", order.getId(), e.getMessage(), e);
        }
    }
}
