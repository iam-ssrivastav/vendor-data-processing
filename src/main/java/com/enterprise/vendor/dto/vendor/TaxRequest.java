package com.enterprise.vendor.dto.vendor;

import com.enterprise.vendor.model.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Tax calculation vendor request DTO.
 * 
 * @author Shivam Srivastav
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRequest {
    private Long orderId;
    private BigDecimal amount;
    private ShippingAddress address;
    private String productCategory;
}
