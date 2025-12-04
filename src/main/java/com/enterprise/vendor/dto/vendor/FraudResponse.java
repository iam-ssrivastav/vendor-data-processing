package com.enterprise.vendor.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fraud detection vendor response DTO.
 * 
 * @author Shivam Srivastav
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudResponse {
    private Double score; // 0.0 (safe) to 1.0 (high risk)
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String recommendation; // APPROVE, REVIEW, DECLINE
    private String checkId;
}
