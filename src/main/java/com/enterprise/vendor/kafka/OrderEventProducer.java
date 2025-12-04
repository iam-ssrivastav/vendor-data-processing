package com.enterprise.vendor.kafka;

import com.enterprise.vendor.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for order events.
 * 
 * @author Shivam Srivastav
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private static final String TOPIC = "order-created-events";

    public void publishOrderCreated(Order order) {
        log.info("Publishing OrderCreatedEvent to Kafka for order: {}", order.getId());

        kafkaTemplate.send(TOPIC, order.getId().toString(), order)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Order event published successfully: {}", order.getId());
                    } else {
                        log.error("Failed to publish order event: {}", ex.getMessage());
                    }
                });
    }
}
