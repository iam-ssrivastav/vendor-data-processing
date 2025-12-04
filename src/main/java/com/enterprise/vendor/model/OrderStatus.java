package com.enterprise.vendor.model;

/**
 * Order status enumeration.
 * 
 * @author Shivam Srivastav
 */
public enum OrderStatus {
    CREATED,
    FRAUD_CHECK_PENDING,
    FRAUD_CHECK_PASSED,
    FRAUD_CHECK_FAILED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
