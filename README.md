# Uber-App — Uber-Style Ride-Hailing Backend

A production-inspired **microservices-based ride-hailing backend** built with Java and Spring Boot.

The project models the core ride-booking workflow:

**Rider Request → Kafka → Driver Matching → Redis GEO → Driver Assignment → Kafka → Ride Lifecycle**

The system uses **Apache Kafka** for asynchronous event-driven communication, **Redis Geospatial indexing** for real-time driver location and proximity search, **MySQL** for ride persistence, and **OpenFeign** for synchronous communication between microservices.

---

## 🏗️ Architecture

```text
                         ┌──────────────────────┐
                         │      Rider App       │
                         └──────────┬───────────┘
                                    │ HTTP
                                    ▼
                         ┌──────────────────────┐
                         │    Ride Service      │
                         │       :8083          │
                         │                      │
                         │ Ride Lifecycle       │
                         │ Fare Estimation      │
                         │ MySQL Persistence    │
                         └──────────┬───────────┘
                                    │
                         Kafka: ride.requested
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │  Matching Service    │
                         │       :8084          │
                         │                      │
                         │ Driver Matching      │
                         │ Kafka Consumer       │
                         └──────────┬───────────┘
                                    │
                              OpenFeign HTTP
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │   Location Service   │
                         │       :8082          │
                         │                      │
                         │ Nearby Driver Search │
                         │ Driver Locations     │
                         └──────────┬───────────┘
                                    │
                                    ▼
                              ┌─────────────┐
                              │    Redis    │
                              │  GEO Index  │
                              └─────────────┘

                         Matching Service
                                │
                         Kafka: ride.matched
                                │
                                ▼
                         ┌──────────────────────┐
                         │    Ride Service      │
                         │ Assign Driver        │
                         │ Update Ride Status   │
                         └──────────────────────┘
```

---

## 🚀 Core Features

### Ride Management

* Create a ride request
* Persist ride details in MySQL
* Estimate ride fare
* Track ride status
* Start a ride
* Complete a ride
* Cancel a ride
* Retrieve ride details
* Retrieve rider ride history

### Real-Time Driver Location

* Driver location updates
* Redis GEO-based location storage
* Radius-based nearby-driver search
* Distance calculation
* Top nearby drivers retrieval
* Driver removal/offline support

### Driver Matching

* Kafka-based asynchronous ride-request processing
* OpenFeign communication with Location Service
* Nearby-driver discovery
* Driver scoring and selection
* Kafka-based ride-match event publishing

### Event-Driven Communication

The project uses Kafka topics for asynchronous communication:

```text
ride.requested
        │
        ▼
Matching Service
        │
        ▼
ride.matched
        │
        ▼
Ride Service
```

---

# 🔄 End-to-End Ride Flow

### 1. Rider requests a ride

The rider sends:

```http
POST /api/v1/rides/request
```

Ride Service:

1. Validates the request.
2. Creates the ride in MySQL.
3. Sets the initial status.
4. Publishes a `RideRequestedEvent`.
5. Sends the event to Kafka topic:

```text
ride.requested
```

---

### 2. Matching Service consumes the event

Matching Service listens to:

```text
ride.requested
```

It receives:

```text
RideRequestedEvent
```

and calls Location Service using OpenFeign.

---

### 3. Location Service searches nearby drivers

Location Service queries Redis GEO using the rider's pickup coordinates.

Example:

```http
GET /api/v1/locations/driver/nearby
```

Redis returns nearby drivers sorted by distance.

Example response:

```json
[
  {
    "driverId": "driver:4",
    "latitude": 26.9125,
    "longitude": 75.7874,
    "distanceInKm": 0.0149
  }
]
```

---

### 4. Matching Service selects a driver

The Matching Service evaluates available nearby drivers and selects a driver using the matching score.

Current implementation uses:

```text
70% → Distance
30% → Simulated Driver Rating
```

> The rating is currently simulated because a dedicated Driver Service is not yet part of this repository.

---

### 5. Ride matched event

Matching Service publishes:

```text
ride.matched
```

containing information such as:

```text
rideId
riderId
driverId
driverLatitude
driverLongitude
distance
```

---

### 6. Ride Service updates the ride

Ride Service consumes `ride.matched` and assigns the selected driver.

The ride moves to:

```text
MATCHING → ACCEPTED
```

---

### 7. Ride lifecycle

The complete tested lifecycle is:

```text
REQUESTED
    ↓
MATCHING
    ↓
ACCEPTED
    ↓
RIDE_STARTED
    ↓
COMPLETED
```

Cancellation is also supported:

```text
REQUESTED / MATCHING / ACCEPTED
                ↓
            CANCELLED
```

---

# 🧩 Microservices

| Service              |   Port | Responsibility                                | Storage   |
| -------------------- | -----: | --------------------------------------------- | --------- |
| **ride-service**     | `8083` | Ride lifecycle, fare estimation, ride history | MySQL     |
| **location-service** | `8082` | Driver GPS location and nearby-driver search  | Redis GEO |
| **matching-service** | `8084` | Driver matching and event processing          | Stateless |

---

# 🛠️ Tech Stack

### Backend

* Java 24
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Data Redis
* Spring Validation
* Spring Kafka
* Spring Cloud OpenFeign
* Spring Boot Actuator
* Lombok

### Infrastructure

* Apache Kafka
* Zookeeper
* Redis
* MySQL
* Docker
* Docker Compose

### Development Tools

* Maven
* IntelliJ IDEA
* Postman
* Git
* GitHub

---

# 📁 Project Structure

```text
Uber-App/
│
├── docker-compose.yml
│
├── location-service/
│   └── location-service/
│       ├── src/
│       └── pom.xml
│
├── matching-service (1)/
│   └── matching-service/
│       ├── src/
│       └── pom.xml
│
├── ride-service/
│   └── ride-service/
│       ├── src/
│       └── pom.xml
│
└── .gitignore
```

---

# ⚙️ Prerequisites

Install the following:

* JDK 24+
* Maven
* Docker Desktop
* Docker Compose
* Git

Verify:

```bash
java -version
mvn -version
docker --version
docker compose version
```

---

# 🐳 Running Infrastructure

From the project root:

```bash
docker compose up -d
```

This starts the infrastructure required by the application:

```text
MySQL       → 3306
Redis       → 6379
Zookeeper   → 2181
Kafka       → 9092
```

Check containers:

```bash
docker ps
```

---

# ▶️ Running the Services

The services are independent Spring Boot applications.

### Location Service

```powershell
cd location-service\location-service
.\mvnw.cmd spring-boot:run
```

Runs on:

```text
http://localhost:8082
```

### Matching Service

```powershell
cd "matching-service (1)\matching-service"
.\mvnw.cmd spring-boot:run
```

Runs on:

```text
http://localhost:8084
```

### Ride Service

```powershell
cd ride-service\ride-service
.\mvnw.cmd spring-boot:run
```

Runs on:

```text
http://localhost:8083
```

---

# 🧪 API Testing

## 1. Register Driver Location

Before requesting a ride, register at least one nearby driver.

```http
POST http://localhost:8082/api/v1/locations/drivers/update
```

Request:

```json
{
  "driverId": "driver:4",
  "latitude": 26.9125,
  "longitude": 75.7874
}
```

---

## 2. Find Nearby Drivers

```http
GET http://localhost:8082/api/v1/locations/driver/nearby?latitude=26.9124&longitude=75.7873&radius=5.0
```

Example response:

```json
[
  {
    "driverId": "driver:4",
    "latitude": 26.912499496045946,
    "longitude": 75.7874009013176,
    "distanceInKm": 0.0149
  }
]
```

---

## 3. Request a Ride

```http
POST http://localhost:8083/api/v1/rides/request
```

Request:

```json
{
  "riderId": "101",
  "pickupLatitude": 26.9124,
  "pickupLongitude": 75.7873,
  "pickupAddress": "Jaipur Railway Station",
  "dropLatitude": 26.8467,
  "dropLongitude": 75.8056,
  "dropAddress": "Malviya Nagar, Jaipur"
}
```

The request triggers:

```text
Ride Service
     ↓
ride.requested
     ↓
Matching Service
     ↓
Location Service
     ↓
Redis GEO
     ↓
Driver Selection
     ↓
ride.matched
     ↓
Ride Service
```

---

# 🚕 Ride Lifecycle APIs

Base URL:

```text
http://localhost:8083/api/v1/rides
```

| Method | Endpoint             | Description            |
| ------ | -------------------- | ---------------------- |
| `POST` | `/request`           | Create ride request    |
| `GET`  | `/{rideId}`          | Get ride details       |
| `GET`  | `/rider/{riderId}`   | Get rider ride history |
| `PUT`  | `/{rideId}/start`    | Start ride             |
| `PUT`  | `/{rideId}/complete` | Complete ride          |
| `PUT`  | `/{rideId}/cancel`   | Cancel ride            |

Example:

```http
PUT http://localhost:8083/api/v1/rides/6/start
```

Complete:

```http
PUT http://localhost:8083/api/v1/rides/6/complete
```

---

# 📍 Location APIs

Base URL:

```text
http://localhost:8082/api/v1/locations
```

| Method   | Endpoint              | Description            |
| -------- | --------------------- | ---------------------- |
| `POST`   | `/drivers/update`     | Update driver location |
| `GET`    | `/driver/nearby`      | Find nearby drivers    |
| `DELETE` | `/drivers/{driverId}` | Remove driver location |

---

# 📡 Kafka Topics

### `ride.requested`

Published by:

```text
Ride Service
```

Consumed by:

```text
Matching Service
```

Purpose:

```text
Notify Matching Service that a rider requested a ride.
```

### `ride.matched`

Published by:

```text
Matching Service
```

Consumed by:

```text
Ride Service
```

Purpose:

```text
Notify Ride Service that a driver has been selected.
```

---

# 🗺️ Redis GEO

Driver locations are stored using Redis Geospatial commands.

Redis GEO index:

```text
drivers:locations
```

Conceptually:

```text
Driver
  │
  ▼
GEOADD
  │
  ▼
Redis GEO Index
  │
  ▼
GEO radius search
  │
  ▼
Nearby Drivers
```

The Location Service searches within a configurable radius and returns up to the nearest 10 drivers.

---

# 🔌 Service Communication

The project demonstrates two different communication patterns.

### Asynchronous — Kafka

```text
Ride Service
     │
     │ ride.requested
     ▼
Matching Service
```

and:

```text
Matching Service
     │
     │ ride.matched
     ▼
Ride Service
```

### Synchronous — OpenFeign

```text
Matching Service
       │
       │ HTTP
       ▼
Location Service
       │
       ▼
     Redis
```

This combination demonstrates both **event-driven architecture** and **synchronous service-to-service communication**.

---

# 🔐 Configuration

For local development, service configuration is stored in each service's Spring configuration.

For production deployments, sensitive values such as:

```text
Database credentials
Kafka credentials
Redis credentials
Service URLs
```

should be supplied through environment variables or a dedicated secrets-management solution.

---

# 📊 Current Project Status

The core backend workflow has been implemented and tested:

```text
✅ Microservices architecture
✅ Spring Boot
✅ Kafka event-driven communication
✅ Redis GEO driver location
✅ Nearby-driver search
✅ OpenFeign communication
✅ MySQL persistence
✅ Ride lifecycle
✅ Fare estimation
✅ Driver matching
✅ Docker infrastructure
✅ Actuator
✅ Git/GitHub
```

A complete ride flow has been successfully tested:

```text
REQUESTED
    ↓
MATCHING
    ↓
ACCEPTED
    ↓
RIDE_STARTED
    ↓
COMPLETED
```

---

# 🚧 Future Improvements

The current implementation focuses on the core backend architecture. Possible production-oriented extensions include:

* [ ] Dedicated Driver Service
* [ ] Real driver profiles and ratings
* [ ] Driver accept/reject workflow
* [ ] Driver availability management
* [ ] Authentication and authorization
* [ ] API Gateway
* [ ] Service discovery
* [ ] Resilience4j circuit breaker and retry
* [ ] Kafka retry and Dead Letter Topics
* [ ] Idempotent event processing
* [ ] Centralized exception handling
* [ ] Integration tests with Testcontainers
* [ ] Prometheus + Grafana monitoring
* [ ] Distributed tracing
* [ ] Externalized configuration and secrets
* [ ] More realistic fare/surge pricing
* [ ] Containerized Spring Boot services
* [ ] CI/CD pipeline

---

# 🎯 What This Project Demonstrates

This project demonstrates practical backend concepts beyond basic CRUD:

```text
Microservices
     +
Event-Driven Architecture
     +
Apache Kafka
     +
Redis Geospatial Search
     +
OpenFeign
     +
MySQL
     +
Docker
     +
Distributed Service Communication
     +
Ride State Management
```

It is designed as a **Java backend engineering project** demonstrating how multiple independent services can work together to implement a real-world ride-hailing workflow.
