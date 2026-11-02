# RideFlow:Microservices Platform

An event-driven, microservices-based ride-hailing backend built with **Java** and **Spring Boot**.

RideFlow models the core ride-booking workflow end to end:

**Rider request → Kafka → Driver matching → Redis GEO search → Driver assignment → Kafka → Ride lifecycle**

| Concern | Technology |
|---|---|
| Asynchronous service-to-service events | Apache Kafka |
| Real-time driver location and proximity search | Redis (in-memory) with GEO indexing |
| Synchronous service-to-service calls | Spring Cloud OpenFeign |
| Ride persistence | **PostgreSQL** via Spring Data JPA |
| Containerization | Docker and Docker Compose (all three services and all infrastructure) |
| Orchestration | Kubernetes (in progress) |

> **Status note: work in progress, commits coming.**
> The following have been completed and tested locally, and will be pushed to this repository in upcoming commits:
> - Persistence migrated from **MySQL to PostgreSQL**
> - **All three microservices and all infrastructure** (PostgreSQL, Redis, Zookeeper, Kafka) containerized with Docker. They run together and communicate over a shared Docker network
> - **Kubernetes** deployment, which is currently in progress
>
> Until then, the code on `main` may still reference MySQL and run the services outside Docker. *(Remove this note once everything is pushed.)*

---

## Architecture

```
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
                         │ PostgreSQL Storage   │
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

## Microservices

| Service | Port | Responsibility | Storage |
|---|---:|---|---|
| `ride-service` | `8083` | Ride lifecycle, fare estimation, ride history | PostgreSQL |
| `location-service` | `8082` | Driver GPS location and nearby-driver search | Redis GEO |
| `matching-service` | `8084` | Driver matching and event processing | Stateless |

## Features

**Ride management**
- Create a ride request and persist it in PostgreSQL
- Estimate ride fare
- Track ride status through its full lifecycle
- Start, complete or cancel a ride
- Retrieve ride details and a rider's ride history

**Real-time driver location**
- Driver location updates stored in a Redis GEO index
- Radius-based nearby-driver search with distance calculation
- Top-N nearest drivers retrieval
- Driver removal / offline support

**Driver matching**
- Kafka-based asynchronous processing of ride requests
- OpenFeign calls to Location Service for nearby drivers
- Driver scoring and selection
- Publishes a ride-matched event back to Kafka

---

## End-to-End Ride Flow

1. **Rider requests a ride.** `POST /api/v1/rides/request`. Ride Service validates the request, saves the ride in PostgreSQL with its initial status, and publishes a `RideRequestedEvent` to the `ride.requested` topic.
2. **Matching Service consumes the event** and calls Location Service through OpenFeign with the pickup coordinates.
3. **Location Service searches Redis GEO** and returns nearby drivers sorted by distance.
4. **Matching Service selects a driver** using a weighted score:
   - 70% distance
   - 30% simulated driver rating

   > The rating is simulated because a dedicated Driver Service is not yet part of this repository.
5. **Matching Service publishes `ride.matched`** containing `rideId`, `riderId`, `driverId`, driver coordinates and distance.
6. **Ride Service consumes `ride.matched`**, assigns the driver, and moves the ride from `MATCHING` to `ACCEPTED`.

### Ride status lifecycle

```
REQUESTED → MATCHING → ACCEPTED → RIDE_STARTED → COMPLETED

REQUESTED / MATCHING / ACCEPTED → CANCELLED
```

---

## Tech Stack

**Backend:** Java 24, Spring Boot, Spring Web, Spring Data JPA, Spring Data Redis, Spring Validation, Spring Kafka, Spring Cloud OpenFeign, Spring Boot Actuator, Lombok

**Infrastructure:** Apache Kafka, Zookeeper, Redis, **PostgreSQL**, **Docker**, **Docker Compose**, Kubernetes (in progress)

**Tools:** Maven, IntelliJ IDEA, Postman, Git, GitHub

## Project Structure

```
RideFlow/
├── docker-compose.yml
├── location-service/
│   └── location-service/
│       ├── src/
│       ├── Dockerfile
│       └── pom.xml
├── matching-service/
│   └── matching-service/
│       ├── src/
│       ├── Dockerfile
│       └── pom.xml
├── ride-service/
│   └── ride-service/
│       ├── src/
│       ├── Dockerfile
│       └── pom.xml
├── k8s/                  (Kubernetes manifests, in progress)
└── .gitignore
```

---

## Getting Started

### Prerequisites

- JDK 24+
- Maven (or use the bundled `mvnw` wrapper)
- Docker Desktop with Docker Compose
- Git

```bash
java -version
mvn -version
docker --version
docker compose version
```

### Option A: Run everything with Docker (recommended)

Every microservice and every piece of infrastructure is containerized. One command builds the service images and starts the full system:

```bash
docker compose up --build -d
```

| Container | Port |
|---|---:|
| `ride-service` | `8083` |
| `matching-service` | `8084` |
| `location-service` | `8082` |
| PostgreSQL | `5432` |
| Redis | `6379` |
| Zookeeper | `2181` |
| Kafka | `9092` |

Inside the Docker network, containers reach each other by **service name** rather than `localhost` (for example, Matching Service calls Location Service by its Compose service name, and every service connects to Kafka, Redis and PostgreSQL by name). This is why the full flow, from ride request to driver assignment, works across containers.

Check that everything is up and watch the logs:

```bash
docker compose ps
docker compose logs -f ride-service
```

Stop everything:

```bash
docker compose down
```

Once the containers are running, jump to [Try It Out](#try-it-out) to exercise the system.

### Option B: Infrastructure in Docker, services with Maven

Useful for development and debugging. Start only the infrastructure containers (PostgreSQL, Redis, Zookeeper, Kafka), then run each service from your IDE or terminal.

```bash
docker compose up -d postgres redis zookeeper kafka
```

Then start each service in its own terminal:

**Location Service** (`http://localhost:8082`)

```bash
cd location-service/location-service
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

**Matching Service** (`http://localhost:8084`)

```bash
cd matching-service/matching-service
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

**Ride Service** (`http://localhost:8083`)

```bash
cd ride-service/ride-service
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

When running outside Docker, services connect to the infrastructure on `localhost`.

---

## Kubernetes (In Progress)

I am currently deploying the same system to Kubernetes. The goal is to run each microservice as its own Deployment with a Service for discovery, ConfigMaps and Secrets for configuration, and the stateful infrastructure (PostgreSQL, Kafka) deployed with persistent storage. Manifests will live in the `k8s/` directory and will be committed once they are working.

---

## PostgreSQL Configuration

Ride Service stores rides in PostgreSQL through Spring Data JPA. The relevant pieces are:

**Maven dependency** (`ride-service/ride-service/pom.xml`)

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Spring properties** (example; match the database name and credentials to your `docker-compose.yml`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<database-name>
spring.datasource.username=<username>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=update
```

Do not commit real credentials. For anything beyond local development, supply database, Kafka and Redis settings through environment variables or a secrets manager.

**Inspect the data**

```bash
docker exec -it <postgres-container-name> psql -U <username> -d <database-name>
```

---

## API Reference

### Ride Service: `http://localhost:8083/api/v1/rides`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/request` | Create a ride request |
| `GET` | `/{rideId}` | Get ride details |
| `GET` | `/rider/{riderId}` | Get a rider's ride history |
| `PUT` | `/{rideId}/start` | Start a ride |
| `PUT` | `/{rideId}/complete` | Complete a ride |
| `PUT` | `/{rideId}/cancel` | Cancel a ride |

### Location Service: `http://localhost:8082/api/v1/locations`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/drivers/update` | Update a driver's location |
| `GET` | `/driver/nearby` | Find nearby drivers |
| `DELETE` | `/drivers/{driverId}` | Remove a driver's location |

---

## Try It Out

**1. Register a nearby driver**

```http
POST http://localhost:8082/api/v1/locations/drivers/update
Content-Type: application/json

{
  "driverId": "driver:4",
  "latitude": 26.9125,
  "longitude": 75.7874
}
```

**2. Find nearby drivers**

```http
GET http://localhost:8082/api/v1/locations/driver/nearby?latitude=26.9124&longitude=75.7873&radius=5.0
```

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

**3. Request a ride**

```http
POST http://localhost:8083/api/v1/rides/request
Content-Type: application/json

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

**4. Move the ride through its lifecycle**

```http
PUT http://localhost:8083/api/v1/rides/{rideId}/start
PUT http://localhost:8083/api/v1/rides/{rideId}/complete
```

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `ride.requested` | Ride Service | Matching Service | A rider has requested a ride |
| `ride.matched` | Matching Service | Ride Service | A driver has been selected |

## Redis GEO

Driver locations are stored with Redis geospatial commands under the key `drivers:locations`. Location Service uses `GEOADD` to update positions and a radius search to find drivers near the pickup point, returning up to the 10 nearest drivers, sorted by distance.

## Service Communication

| Pattern | Used for |
|---|---|
| **Asynchronous (Kafka)** | Ride Service ↔ Matching Service events |
| **Synchronous (OpenFeign)** | Matching Service → Location Service |

---

## Current Status

Implemented and tested end to end:

- Microservices architecture with Spring Boot
- Kafka event-driven communication
- Redis GEO driver location and nearby-driver search
- OpenFeign service-to-service calls
- PostgreSQL persistence (migrated from MySQL)
- Full ride lifecycle and fare estimation
- Driver matching
- Dockerized microservices and infrastructure, running and communicating together with Docker Compose
- Spring Boot Actuator

## Roadmap

- Dedicated Driver Service with real profiles and ratings
- Driver accept / reject workflow and availability management
- Authentication and authorization
- API Gateway and service discovery
- Resilience4j circuit breaker and retry
- Kafka retry, dead-letter topics and idempotent event processing
- Centralized exception handling
- Integration tests with Testcontainers
- Prometheus and Grafana monitoring, distributed tracing
- Externalized configuration and secrets
- Surge pricing and more realistic fare calculation
- Kubernetes deployment (in progress)
- CI/CD pipeline

## What This Project Demonstrates

Practical backend engineering beyond basic CRUD: microservices, event-driven architecture, Apache Kafka, Redis geospatial search, service-to-service communication, relational persistence with PostgreSQL, a fully containerized system (Docker and Docker Compose, with Kubernetes in progress), and ride state management.
