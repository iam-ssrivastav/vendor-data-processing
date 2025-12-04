package com.enterprise.vendor.dto;

import com.enterprise.vendor.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for order information.
 * 
 * @author Shivam Srivastav
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal amount;
    private OrderStatus status;
    private BigDecimal taxAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private Double fraudScore;
    private String paymentTransactionId;
    private String paymentStatus;
    private String shippingTrackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
