# Vendor Data Processing System - Testing Guide

## Quick Test

### 1. Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "LAPTOP-PRO",
    "quantity": 2,
    "amount": 1999.99,
    "shippingAddress": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    }
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "customerId": "CUST-001",
  "productId": "LAPTOP-PRO",
  "quantity": 2,
  "amount": 1999.99,
  "status": "CREATED",
  "createdAt": "2025-12-02T22:45:00"
}
```

### 2. Check Order Status (after a few seconds)

```bash
curl http://localhost:8080/api/orders/1
```

**Expected Response (after vendor processing):**
```json
{
  "id": 1,
  "customerId": "CUST-001",
  "productId": "LAPTOP-PRO",
  "quantity": 2,
  "amount": 1999.99,
  "status": "PAYMENT_COMPLETED",
  "taxAmount": 160.00,
  "shippingCost": 9.99,
  "totalAmount": 2169.98,
  "fraudScore": 0.15,
  "paymentTransactionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "paymentStatus": "SUCCESS",
  "shippingTrackingNumber": "TRACK-ABC12345",
  "createdAt": "2025-12-02T22:45:00",
  "updatedAt": "2025-12-02T22:45:05"
}
```

### 3. Get Customer Orders

```bash
curl http://localhost:8080/api/orders/customer/CUST-001
```

## Test Different Scenarios

### High-Risk Order (Fraud Detection)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-FRAUD",
    "productId": "EXPENSIVE-ITEM",
    "quantity": 10,
    "amount": 99999.99,
    "shippingAddress": {
      "street": "456 Suspicious St",
      "city": "Los Angeles",
      "state": "CA",
      "zipCode": "90001",
      "country": "USA"
    }
  }'
```

### Express Shipping

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-EXPRESS",
    "productId": "URGENT-ITEM",
    "quantity": 1,
    "amount": 299.99,
    "shippingAddress": {
      "street": "789 Fast Lane",
      "city": "Austin",
      "state": "TX",
      "zipCode": "73301",
      "country": "USA"
    }
  }'
```

## Monitor System

### Check Kafka Topics

```bash
docker exec kafka-vendor kafka-topics --list --bootstrap-server localhost:9092
```

### View Kafka Messages

```bash
docker exec kafka-vendor kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-created-events \
  --from-beginning \
  --max-messages 5
```

### Check Circuit Breaker Status

```bash
curl http://localhost:8080/actuator/circuitbreakers
```

### View Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

## Expected Data Flow

1. **Order Created** → Saved to H2 database
2. **Kafka Event Published** → `order-created-events` topic
3. **Kafka Consumer Triggered** → VendorOrchestrator starts
4. **Fraud Check** → Mock vendor returns score (0.0-1.0)
5. **Tax Calculation** → Based on state (NY=8%, CA=7.25%, etc.)
6. **Shipping Calculation** → Based on service type
7. **Payment Processing** → 80% success rate simulation
8. **Order Updated** → Final status and totals saved

## Troubleshooting

### Kafka Not Ready
```bash
docker-compose restart kafka
```

### View Application Logs
```bash
tail -f logs/application.log
```

### Reset Database
```bash
# H2 is in-memory, just restart the app
./mvnw spring-boot:run
```

## Performance Testing

### Create 10 Orders Simultaneously

```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"CUST-$i\",
      \"productId\": \"PROD-$i\",
      \"quantity\": 1,
      \"amount\": 99.99,
      \"shippingAddress\": {
        \"street\": \"$i Main St\",
        \"city\": \"New York\",
        \"state\": \"NY\",
        \"zipCode\": \"10001\",
        \"country\": \"USA\"
      }
    }" &
done
wait
```

---

**Author:** Shivam Srivastav
