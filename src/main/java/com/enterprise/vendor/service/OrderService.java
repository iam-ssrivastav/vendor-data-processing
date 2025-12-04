package com.enterprise.vendor.service;

import com.enterprise.vendor.dto.OrderRequest;
import com.enterprise.vendor.dto.OrderResponse;
import com.enterprise.vendor.kafka.OrderEventProducer;
import com.enterprise.vendor.model.Order;
import com.enterprise.vendor.model.OrderStatus;
import com.enterprise.vendor.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Service - handles order creation and management.
 * 
 * @author Shivam Srivastav
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        // Create order entity
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .shippingAddress(request.getShippingAddress())
                .status(OrderStatus.CREATED)
                .build();

        order = orderRepository.save(order);
        log.info("Order created with ID: {}", order.getId());

        // Publish event to Kafka for async vendor processing
        eventProducer.publishOrderCreated(order);

        return mapToResponse(order);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    public List<OrderResponse> getCustomerOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, String transactionId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setPaymentTransactionId(transactionId);
        order.setPaymentStatus(status);

        if ("SUCCESS".equals(status)) {
            order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        } else if ("FAILED".equals(status)) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
        }

        orderRepository.save(order);
        log.info("Updated payment status for order {}: {}", orderId, status);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .status(order.getStatus())
                .taxAmount(order.getTaxAmount())
                .shippingCost(order.getShippingCost())
                .totalAmount(order.getTotalAmount())
                .fraudScore(order.getFraudScore())
                .paymentTransactionId(order.getPaymentTransactionId())
                .paymentStatus(order.getPaymentStatus())
                .shippingTrackingNumber(order.getShippingTrackingNumber())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
