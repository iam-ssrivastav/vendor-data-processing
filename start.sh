#!/bin/bash

echo "🚀 Starting Vendor Data Processing System..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Start infrastructure
echo "📦 Starting Kafka and PostgreSQL..."
docker-compose up -d

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
sleep 10

# Check if Kafka is ready
until docker exec kafka-vendor kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
    echo "Waiting for Kafka..."
    sleep 2
done

echo "✅ Kafka is ready!"
echo ""

# Build the application
echo "🔨 Building application..."
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"
echo ""

# Run the application
echo "🚀 Starting Spring Boot application..."
echo "📍 Application will be available at: http://localhost:8080"
echo "📍 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "📍 H2 Console: http://localhost:8080/h2-console"
echo ""

./mvnw spring-boot:run
