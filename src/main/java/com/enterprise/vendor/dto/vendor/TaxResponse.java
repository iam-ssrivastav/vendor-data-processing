package com.enterprise.vendor.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Tax calculation vendor response DTO.
 * 
 * @author Shivam Srivastav
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxResponse {
    private BigDecimal taxAmount;
    private BigDecimal taxRate;
    private String jurisdiction;
}
