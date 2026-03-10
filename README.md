# Scalable Ticket System

This project is a private playground for learning concepts and technologies for high-available distributed systems. It implements a cloud-native ticketing system designed to handle extreme traffic spikes through distributed processing and caching.

## Fictional Use Cases

1. **Check ticket availability (High Frequency Read)**
2. **Buy tickets (High Concurrency Write)**

![System Overview of the Scalable Ticket System](scalable-ticket-system-overview.png)

## Infrastructure components

- **Ticket-API (2 Replicas):** Spring Boot Service (REST API) handling HTTP requests. Performs high-speed reads via Redis and publishes write-events to RabbitMQ.
- **Ticket-Worker (2 Replicas):** Spring Boot backend consumer. Asynchronously processes orders, updates the database and invalidates the cache.
- **Load Balancer:** Kubernetes Service distributing incoming traffic across API replicas using a Round-Robin strategy.
- **Redis:** In-memory store used as a Look-Aside Cache (TTL: 10s) to reduce read-load on the database.
- **PostgreSQL:** Primary relational database for persistent storage of ticket inventory and order transactions.
- **RabbitMQ:** Message broker that buffers high-concurrency write requests.
- **Prometheus:** Scrapes metrics from application endpoints via Spring Boot Actuator.
- **Grafana:** Visualization layer connected to Prometheus to monitor system health and bottlenecks in real-time.

## Key Architectural Decisions

- **Command Query Responsibility Segregation (CQRS):** Separation of read operations (API + Cache) and write operations (Worker + DB) to optimize for different load profiles.
- **Eventual Consistency:** User requests are acknowledged immediately (HTTP 202), while the actual data consistency is ensured asynchronously by the worker.
- **Resilience & Scalability & Asynchronous Decoupling:** Through replication, load balancing, self-healing (automatic restart of failed pods), an event-driven architecture with a message queue and Resilience4j (timeouts, circuit breakers, bulkheads).
- **Object-Oriented Programming (OOP) & Domain-Driven Design (DDD):** Implementation of domain-centric logic and OOP principles to ensure high code reusability, modular interchangeability and maintainability.

## Folder Structure

The backend is organized as a Maven multi-module project.

- **backend:** Root folder for Spring Boot services.
  - **ticket-api:** REST API handling HTTP requests.
  - **ticket-worker:** Consumer for asynchronous processing.
  - **ticket-common:** Shared library containing DTOs, Repositories and utilities.
- **k8s:** Kubernetes deployment configurations.
  - **apps:** Manifests for application services.
  - **infrastructure:** Manifests for system infrastructure.

## How to start locally:

### Prerequisites
- **Docker & Docker Compose**
- **Kubectl** (for Kubernetes deployment)
- **Minikube** (for a local Kubernetes Cluster)
- **Helm** ([Installation](https://helm.sh/docs/intro/install/))
- *Optional:* **Task** ([Installation](https://taskfile.dev/docs/installation))
- *Optional:* **Java 21** (only for local development)

### 1. Environment Setup
Create a `.env` file from the example:
```bash
cp .env.example .env
```
> [!IMPORTANT]
> Change the default secrets in `.env` before any non-local deployment!

### 2. Execution Modes

#### Option A: Local Kubernetes

1.  **Minikube Setup:**
    > [!IMPORTANT]
    > Docker must be running before starting this step!
    ```bash
    task minikube:setup
    ```
    This will start Minikube with the docker driver, enable the ingress addon, and load the backend images into the cluster.

2.  **Deploy Applications:**
    ```bash
    # Generate K8s secrets, configmap from .env and deploys infrastructure and applications in local kubernetes cluster
    task k8s:deploy
    ```

3.  **Access the Cluster:**
    To access the services via the Ingress (localhost), you must start the Minikube tunnel in a separate terminal:
    ```bash
    task minikube:tunnel
    ```
    **Swagger UI (K8s):** [http://localhost/api/swagger-ui/index.html](http://localhost/api/swagger-ui/index.html)

#### Option B: Local Development (IDE + Docker Single Instance Infrastructure)
1. **Start Infrastructure:** Run `docker-compose up -d` to start Postgres, Redis, and RabbitMQ.
2. **IDE Run-Configuration:**
   - **Active Profile:** Set `spring.profiles.active=dev`.
   - **Environment Variables:** Load variables from `.env` (e.g., using the 'EnvFile' plugin for IntelliJ or launch.json in VS Code).
3. **Build & Run:** 
   ```bash
   task build  # Or: cd backend && ./mvnw clean install
   ```
   Run `TicketApiApplication` or `TicketWorkerApplication` directly from your IDE.

   **Swagger UI (Local):** [http://localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html)

## Planned ideas for the future:

- Deploy on Azure with CI/CD Pipeline
- JWT Authentication
- Simple Frontend with React

