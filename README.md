# Digital Wallet Platform

A **scalable, secure, high-throughput digital wallet and payment platform** built using **microservices**
and an **event-driven architecture**. The system is optimized to handle high **TPS** with safe concurrent wallet
operations and reliable distributed payment processing.

---

## Tech Stack

- **Language:** Java  
- **Framework:** Spring Boot (Microservices)  
- **Messaging:** Apache Kafka (Event-Driven Communication)  
- **Database:** H2DB  
- **Containerization:** Docker, Docker Compose  
- **Security:** JWT, Role-Based Access Control (RBAC)  
- **API Layer:** Spring Cloud Gateway (API Gateway)

---

## System Architecture

Client → API Gateway → Wallet Core Service  
Wallet Core Service → Transaction Service (Create Transaction)  
Transaction Service → Kafka → Reward & Notification Services (Async consumers)  

---

## Microservices

| Service | Responsibility |
|--------|---------------|
| **API Gateway** | Request routing, JWT validation, filtering |
| **User Service** | Auth, roles, JWT generation, RBAC |
| **Transaction Service** | Persists transactions (H2DB), publishes Kafka events |
| **Reward Service** | Processes reward events, credits wallets |
| **Notification Service** | Sends async notifications from Kafka events |
| **DigitalWalletSystem (Wallet Core)** | Concurrency-safe wallet operations, reward logic |

---

## High TPS & Concurrency Design

- Uses **per-wallet `StampedLock`** for fine-grained concurrency control.  
- Implements **consistent lock ordering** to prevent deadlocks.  
- Uses **`ConcurrentHashMap`** for fast, lock-free wallet lock lookup.  
- Wallet state updates stay **local and thread-safe**, while transaction persistence is handled by a dedicated service.

---

## Kafka Event Flow

Wallet actions trigger transaction persistence, which emits events to Kafka for downstream processing.

---

## Deployment & Setup

### Start all services + Kafka infra:
```bash
docker-compose up -d

