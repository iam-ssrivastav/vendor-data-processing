package com.enterprise.vendor.dto.vendor;

import com.enterprise.vendor.model.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shipping vendor request DTO.
 * 
 * @author Shivam Srivastav
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRequest {
    private Long orderId;
    private ShippingAddress fromAddress;
    private ShippingAddress toAddress;
    private Double weight;
    private String serviceType; // STANDARD, EXPRESS, OVERNIGHT
}
