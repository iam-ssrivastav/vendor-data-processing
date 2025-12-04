# Vendor Data Processing System - Interview Explanation Guide

## 🎯 Project Overview

This Spring Boot project demonstrates **real-world external vendor integration** with **data processing and streaming** using **Apache Kafka**. It's designed to answer the interview question: **"How do you handle vendor integration for data processing?"**

---

## 📊 Architecture Summary

```
Client → REST API → Kafka → Vendor Orchestrator → External Vendors (HTTP)
                                                        ↓
                                                    Webhooks
                                                        ↓
                                                   Kafka Events
```

### **Key Components:**

1. **Order Service**: Creates orders and publishes to Kafka
2. **Kafka**: Event streaming platform for async processing
3. **Vendor Orchestrator**: Coordinates calls to multiple vendors
4. **Vendor Clients**: HTTP clients with circuit breaker & retry
5. **Mock Vendors**: Simulates external APIs (Payment, Shipping, Tax, Fraud)
6. **Webhook Controller**: Receives async vendor callbacks

---

## 🔑 Interview Talking Points

### **Q1: "How do you integrate with external vendors?"**

**Answer:**
> "We use a **multi-layered approach** with **event-driven architecture**:
>
> 1. **Async Processing via Kafka**: When an order is created, we publish an event to Kafka instead of blocking the API response. This ensures fast response times.
>
> 2. **Vendor Orchestrator Pattern**: A dedicated service coordinates calls to multiple vendors (Payment, Shipping, Tax, Fraud Detection) in the correct sequence.
>
> 3. **Circuit Breaker & Retry**: We use Resilience4j to handle vendor failures gracefully. If a vendor is down, the circuit opens and we use fallback methods.
>
> 4. **Webhook Handling**: For async vendors (like payment processing), we provide webhook endpoints to receive callbacks when processing completes.
>
> 5. **Vendor Abstraction**: Each vendor has its own client class, making it easy to swap vendors or add new ones without changing business logic."

---

### **Q2: "How do you handle vendor failures?"**

**Answer:**
> "We implement **multiple fault tolerance patterns**:
>
> **Circuit Breaker Pattern:**
> - Monitors vendor API calls in a sliding window (10 requests)
> - Opens circuit after 50% failure rate
> - Prevents cascading failures
> - Auto-recovers after configured wait time
>
> **Retry Pattern:**
> - Exponential backoff (1s → 2s → 4s)
> - Max 3 attempts
> - Only retries on transient errors (timeouts, connection issues)
>
> **Fallback Methods:**
> - Payment: Queue for async retry via Kafka
> - Shipping: Use default rate ($9.99)
> - Tax: Use default 8% rate
> - Fraud: Conservative approval with manual review flag
>
> **Example Code:**
> ```java
> @CircuitBreaker(name = "paymentVendor", fallbackMethod = "fallbackProcessPayment")
> @Retry(name = "paymentVendor")
> public PaymentResponse processPayment(PaymentRequest request) {
>     return paymentVendorClient.charge(request);
> }
>
> public PaymentResponse fallbackProcessPayment(PaymentRequest request, Exception e) {
>     // Queue to Kafka for async retry
>     kafkaProducer.publishPaymentRetry(request);
>     return PaymentResponse.pending();
> }
> ```

---

### **Q3: "How do you handle data streaming?"**

**Answer:**
> "We use **Apache Kafka** for event-driven data streaming:
>
> **Event Flow:**
> 1. Order created → Publish `OrderCreatedEvent` to Kafka
> 2. Kafka consumer picks up event → Triggers vendor processing
> 3. Vendor responses → Update order in database
> 4. Payment webhook → Publish `PaymentCompletedEvent`
>
> **Benefits:**
> - **Decoupling**: API doesn't wait for vendor responses
> - **Scalability**: Can process thousands of orders/second
> - **Reliability**: Messages persist in Kafka until processed
> - **Async Processing**: Long-running vendor calls don't block
>
> **Kafka Topics:**
> - `order-created-events`: New orders
> - `payment-events`: Payment status updates
> - `fraud-events`: Fraud check results
>
> **Consumer Groups:**
> - `vendor-processing-group`: Processes orders with vendors
> - Each service has its own consumer group for parallel processing"

---

### **Q4: "How do you coordinate multiple vendors?"**

**Answer:**
> "We use a **Vendor Orchestrator** pattern:
>
> **Processing Sequence:**
> ```
> 1. Fraud Check (async) → Score: 0.0-1.0
>    ↓ (if approved)
> 2. Tax Calculation (sync) → Based on state
>    ↓
> 3. Shipping Rate (sync) → Based on service type
>    ↓
> 4. Payment Processing (async) → Webhook callback
> ```
>
> **Orchestrator Logic:**
> ```java
> @Async
> @Transactional
> public void processOrderWithVendors(Order order) {
>     // Step 1: Fraud check
>     FraudResponse fraud = fraudClient.checkFraud(order);
>     if (fraud.isDeclined()) {
>         order.setStatus(FRAUD_CHECK_FAILED);
>         return;
>     }
>     
>     // Step 2 & 3: Parallel calls for tax and shipping
>     TaxResponse tax = taxClient.calculateTax(order);
>     ShippingResponse shipping = shippingClient.calculateRate(order);
>     
>     // Step 4: Process payment with total amount
>     BigDecimal total = order.getAmount() + tax + shipping;
>     PaymentResponse payment = paymentClient.processPayment(total);
> }
> ```
>
> **Key Features:**
> - Sequential processing where needed (fraud before payment)
> - Parallel processing where possible (tax + shipping)
> - Transaction management for data consistency
> - Error handling at each step"

---

### **Q5: "How do you handle async vendor responses?"**

**Answer:**
> "We use **webhooks** for async vendor callbacks:
>
> **Webhook Flow:**
> ```
> 1. We call vendor API → Vendor returns 202 Accepted
> 2. Vendor processes async → Takes 5-30 seconds
> 3. Vendor calls our webhook → POST /webhooks/payment
> 4. We update order status → Publish Kafka event
> ```
>
> **Webhook Controller:**
> ```java
> @PostMapping("/webhooks/payment")
> public ResponseEntity<Void> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
>     Long orderId = payload.get("orderId");
>     String transactionId = payload.get("transactionId");
>     String status = payload.get("status");
>     
>     orderService.updatePaymentStatus(orderId, transactionId, status);
>     
>     return ResponseEntity.ok().build();
> }
> ```
>
> **Security:**
> - Webhook signature verification
> - API key validation
> - Idempotency checks (prevent duplicate processing)
> - Rate limiting"

---

### **Q6: "How do you monitor vendor integrations?"**

**Answer:**
> "We have **comprehensive observability**:
>
> **Metrics (Prometheus):**
> - Vendor API call duration
> - Success/failure rates
> - Circuit breaker state
> - Kafka consumer lag
>
> **Circuit Breaker Monitoring:**
> ```bash
> GET /actuator/circuitbreakers
> ```
> Shows: CLOSED, OPEN, HALF_OPEN states
>
> **Logging:**
> - Structured JSON logs with correlation IDs
> - Every vendor call logged with duration
> - Errors logged with stack traces
>
> **Alerts:**
> - Circuit breaker opens → PagerDuty alert
> - Kafka consumer lag > 1000 → Alert
> - Vendor response time > 5s → Warning"

---

## 🏗️ Technical Implementation Details

### **1. Vendor Client Pattern**

Each vendor has a dedicated client class:

```java
@Component
public class PaymentVendorClient {
    private final WebClient webClient;
    
    @CircuitBreaker(name = "paymentVendor", fallbackMethod = "fallback")
    @Retry(name = "paymentVendor")
    public PaymentResponse processPayment(PaymentRequest request) {
        return webClient.post()
            .uri("/charge")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResponse.class)
            .timeout(Duration.ofSeconds(5))
            .block();
    }
}
```

**Benefits:**
- ✅ Single Responsibility Principle
- ✅ Easy to test (mock vendor responses)
- ✅ Easy to swap vendors
- ✅ Centralized configuration

---

### **2. Event-Driven Architecture**

**Producer:**
```java
@Component
public class OrderEventProducer {
    public void publishOrderCreated(Order order) {
        kafkaTemplate.send("order-created-events", order.getId(), order);
    }
}
```

**Consumer:**
```java
@Component
public class OrderEventConsumer {
    @KafkaListener(topics = "order-created-events", groupId = "vendor-processing-group")
    public void consumeOrderCreated(Order order) {
        vendorOrchestrator.processOrderWithVendors(order);
    }
}
```

---

### **3. Resilience Configuration**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentVendor:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  
  retry:
    instances:
      paymentVendor:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
```

---

## 📈 Scalability Strategy

### **Horizontal Scaling:**
- Multiple instances of Spring Boot app
- Kafka partitions for parallel processing
- Consumer groups for load distribution

### **Performance Optimizations:**
- Async processing (non-blocking)
- Connection pooling for HTTP clients
- Database connection pooling
- Caching for frequently accessed data

### **Capacity Planning:**
- **Throughput**: 10,000 orders/second
- **Latency**: API response < 100ms
- **Vendor calls**: Async, don't block API
- **Kafka**: 3 partitions per topic

---

## 🧪 Testing Strategy

### **Unit Tests:**
- Mock vendor clients
- Test circuit breaker behavior
- Test retry logic

### **Integration Tests:**
- Use embedded Kafka
- Use WireMock for vendor APIs
- Test end-to-end flow

### **Load Tests:**
- JMeter/Gatling for performance testing
- Simulate vendor failures
- Test circuit breaker under load

---

## 🎓 Key Learnings for Interview

1. **Vendor Integration is NOT just HTTP calls**
   - Need circuit breakers, retries, fallbacks
   - Need async processing for scalability
   - Need monitoring and observability

2. **Event-Driven Architecture is Essential**
   - Decouples services
   - Enables async processing
   - Provides audit trail

3. **Fault Tolerance is Critical**
   - Vendors WILL fail
   - Need graceful degradation
   - Need to prevent cascading failures

4. **Observability is Key**
   - Monitor circuit breaker states
   - Track vendor response times
   - Alert on anomalies

---

## 📚 Technologies Used

| Technology | Purpose |
|------------|---------|
| **Spring Boot 3.2** | Application framework |
| **Apache Kafka** | Event streaming |
| **Resilience4j** | Circuit breaker, retry |
| **WebClient** | Async HTTP calls |
| **H2 Database** | In-memory storage |
| **Swagger/OpenAPI** | API documentation |
| **Prometheus** | Metrics collection |
| **Docker Compose** | Infrastructure |

---

## 🚀 Running the Project

```bash
# Start infrastructure
docker-compose up -d

# Run application
./start.sh

# Test
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "LAPTOP",
    "quantity": 1,
    "amount": 999.99,
    "shippingAddress": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    }
  }'
```

---

## 💡 Interview Pro Tips

1. **Start with the big picture**: Event-driven architecture
2. **Explain the why**: Why Kafka? Why circuit breakers?
3. **Show trade-offs**: Eventual consistency vs strong consistency
4. **Mention real vendors**: Stripe, Avalara, FedEx APIs
5. **Discuss scalability**: How to handle 1M orders/day
6. **Talk about monitoring**: How you detect issues
7. **Show code**: Be ready to write circuit breaker code

---

**Author:** Shivam Srivastav

**Perfect for interviews at:** Amazon, Google, Microsoft, Stripe, Uber, etc.
