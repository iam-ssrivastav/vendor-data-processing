package com.enterprise.vendor.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Fraud detection vendor request DTO.
 * 
 * @author Shivam Srivastav
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRequest {
    private Long orderId;
    private String customerId;
    private BigDecimal amount;
    private String ipAddress;
    private String email;
    private String callbackUrl;
}
