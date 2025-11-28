# Ecommerce Analytics API

## Overview
This Spring Boot application provides REST APIs for ingesting user events and retrieving analytics metrics such as active users, page views, and active sessions. It uses Redis for fast data storage and Kafka for event streaming.

## Prerequisites
- Java 21 or higher
- Docker (for running Redis and Kafka)
- Gradle

## Setup Instructions

### 1. Clone the Repository
```
git clone https://github.com/aritras25/EcommerceAnalytics.git
cd EcommerceAnalytics_LiftLab/EcommerceAnalytics
```

### 2.a. Start Dependencies (Redis & Kafka)
Start required services using Docker Compose:
```
docker-compose up -d
```
### 2.b. Create Kafka Topic
````
docker-compose exec kafka \
  kafka-topics --create \
  --topic user_events \
  --bootstrap-server kafka:9092 \
  --partitions 3 \
  --replication-factor 1
````

### 3. Build the Application
```
./gradlew build
```

### 4. Run the Application
```
./gradlew bootRun
```
The application will start on `http://localhost:8085` by default.

## API Endpoints & Example CURLs

### 1. Ingest User Event
`POST /apis/v1/events`
```bash
curl -X POST http://localhost:8085/ecommerce-analytics/apis/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "b3b1c2d4-1234-5678-9abc-def012345678",
    "pageUrl": "/home",
    "eventTimestamp": "2025-11-28T12:00:00Z",
    "sessionId": "session-xyz",
    "eventType": "PAGE_VIEW"
  }'
```

### 2. Get Active Users (last 5 minutes)
`GET /apis/v1/metrics/active-users`
```bash
curl http://localhost:8085/ecommerce-analytics/apis/v1/metrics/active-users
```

### 3. Get Page Views (last 15 minutes)
`GET /apis/v1/metrics/pageviews?url=<pageUrl>`
```bash
curl "http://localhost:8085/ecommerce-analytics/apis/v1/metrics/pageviews?url=/home"
```

### 4. Get Active Sessions for a User (last 5 minutes)
`GET /apis/v1/metrics/active-sessions?userId=<userId>`
```bash
curl "http://localhost:8085/ecommerce-analytics/apis/v1/metrics/active-sessions?userId=b3b1c2d4-1234-5678-9abc-def012345678"
```

## Notes
- Ensure Redis and Kafka are running before starting the application.
- All timestamps should be in ISO 8601 format (e.g., `2025-11-28T12:00:00Z`).
- For event ingestion, `eventType` can be values like `PAGE_VIEW`, `CLICK`, etc.
- API responses are in JSON format.

## Troubleshooting
- If you encounter connection errors, verify that Redis and Kafka containers are running.

## Developer
Aritra Saha

