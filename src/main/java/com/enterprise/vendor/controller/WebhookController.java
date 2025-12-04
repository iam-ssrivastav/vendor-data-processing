package com.enterprise.vendor.controller;

import com.enterprise.vendor.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook controller for receiving async vendor callbacks.
 * 
 * Vendors call these endpoints when their async processing completes.
 * 
 * @author Shivam Srivastav
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Vendor callback endpoints")
public class WebhookController {

    private final OrderService orderService;

    @PostMapping("/payment")
    @Operation(summary = "Payment vendor webhook", description = "Receives payment completion callbacks from payment vendor")
    public ResponseEntity<Void> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received payment webhook: {}", payload);

        try {
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            String transactionId = payload.get("transactionId").toString();
            String status = payload.get("status").toString();

            orderService.updatePaymentStatus(orderId, transactionId, status);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing payment webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/fraud")
    @Operation(summary = "Fraud detection webhook", description = "Receives fraud check results from fraud vendor")
    public ResponseEntity<Void> handleFraudWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received fraud webhook: {}", payload);

        // In a real system, you would update order fraud status here
        // For this demo, fraud check is synchronous

        return ResponseEntity.ok().build();
    }
}
