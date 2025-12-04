package com.enterprise.vendor.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Shipping vendor response DTO.
 * 
 * @author Shivam Srivastav
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingResponse {
    private BigDecimal cost;
    private String trackingNumber;
    private Integer estimatedDays;
    private String carrier;
}
