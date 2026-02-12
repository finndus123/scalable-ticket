# Project: Scalable Ticket Service Learning Path

**Purpose:** This document breaks down the complex challenge of building the distributed system into smaller, manageable sub-tasks to ensure a systematic implementation approach.

## Phase 1: Local Kubernetes Setup & Basic Deployment

**Goal:** Move the Spring Boot application from running as a standalone JAR/Docker container to running as a Pod inside Kubernetes.

1.  Install **Minikube** (or Docker Desktop Kubernetes).
2.  Create a simple Spring Boot app (`ticket-api`) with an endpoint `GET /tickets/available`. (Hardcode a return value for now, e.g., "100").
3.  Create a `Dockerfile` and build the image.
4.  Write K8s Manifests:
    * `deployment.yaml` (Defines the Pod).
    * `service.yaml` (Type: `LoadBalancer` or `NodePort` to expose the service externally).
5.  Deploy everything to your local cluster.

**Definition of Done:**
- [ ] `kubectl get pods` shows the status `Running`.
- [ ] A `curl localhost:PORT/tickets/available` returns the hardcoded result from the cluster.

---

## Phase 2: Database & Caching (Redis)

**Goal:** Accelerate read access to offload the database.

1.  Deploy **PostgreSQL** and **Redis** in the cluster (Use Helm Charts or simple Deployment YAMLs).
2.  Extend the `ticket-api`:
    * Store the ticket count in Postgres.
    * On `GET /tickets/available`: Check the Redis Cache first.
    * *Cache Miss:* Load from DB, write to Redis (TTL: 10 seconds), return.
    * *Cache Hit:* Return directly from Redis.

**Definition of Done:**
- [ ] On the first request, you see a log entry "Fetching from DB".
- [ ] On the second request (within 10s), you see "Fetching from Redis".
- [ ] If you delete the Postgres Pod, the API continues to answer for 10s (thanks to the cache).

---

## Phase 3: Asynchronous Processing (Queuing)

**Goal:** Decouple write access. If 1000 users click simultaneously, the DB must not crash immediately.

1.  Deploy **RabbitMQ** in the cluster.
2.  Create the second service: `ticket-worker`.
3.  Extend `ticket-api` with `POST /tickets/buy`:
    * Instead of writing to the DB directly, send an Event (JSON: `{userId: 1, amount: 1}`) to a RabbitMQ Queue (`ticket_orders`).
    * Respond to the user immediately: `HTTP 202 Accepted` ("We are processing your order").
4.  Implement the `ticket-worker`:
    * Listens to the queue `ticket_orders`.
    * Takes the message, simulates work (e.g., `Thread.sleep(100)`), and reduces the stock in the Postgres DB.
    * Invalidates (deletes) the Redis Cache entry for available tickets.

**Definition of Done:**
- [ ] You send a POST request to the API.
- [ ] The API responds immediately.
- [ ] In the `ticket-worker` logs, you see with a slight delay: "Processing order... Database updated."

---

## Phase 4: Replication & Resilience

**Goal:** The system survives if a service crashes and can distribute higher loads.

1.  Modify `deployment.yaml` of `ticket-api`: Set `replicas: 3`.
2.  Modify `deployment.yaml` of `ticket-worker`: Set `replicas: 2`.
3.  Manually kill one of the API pods (`kubectl delete pod <name>`).

**Definition of Done:**
- [ ] `kubectl get pods` shows 3 API instances.
- [ ] When you delete a pod, Kubernetes **automatically** starts a new one (Self-Healing).
- [ ] While the pod is restarting, you can still send requests to the API (the Service routes to the remaining 2 pods).

---

## Phase 5: Monitoring & Visualization

**Goal:** See what is happening inside the cluster without digging through logs.

1.  Integrate **Spring Boot Actuator** and **Micrometer Prometheus** into your `pom.xml`.
2.  Deploy **Prometheus** in the cluster and configure it to "scrape" all services via the `/actuator/prometheus` path.
3.  Deploy **Grafana**. Connect it to Prometheus as a Data Source.
4.  Build (or import) a dashboard displaying:
    * Requests per Second (RPS).
    * HTTP Error Rate.
    * Processing Duration (Latency).
    * Number of messages in the RabbitMQ Queue.

**Definition of Done:**
- [ ] You see curves in Grafana when generating traffic.
- [ ] You can see how many API replicas are currently running.

---

## Phase 6: Security & Hardening

**Goal:** Minimize the Attack Surface.

1.  **Management Port Isolation:** * Configure Spring Boot so that Actuator endpoints are on port `9001`.
    * Ensure your K8s Service for the API only exposes port `8080` externally (via Ingress).
2.  **Network Policies:**
    * Create a policy prohibiting direct internet access to the database (`Postgres`).
    * Allow access to Postgres **only** from pods with the labels `app: ticket-worker` and `app: ticket-api`.
3.  **Secrets Management:**
    * Do not hardcode passwords for Postgres or RabbitMQ in `values.yaml` or `deployment.yaml`.
    * Use **Kubernetes Secrets** and inject them as environment variables into your pods.

**Definition of Done:**
- [ ] A `curl` to `localhost:8080/actuator` fails from the outside.
- [ ] Prometheus can still collect metrics internally via port `9001`.
- [ ] A temporary "attacker pod" in the cluster cannot ping the database unless it has the correct labels.

---

## Phase 7: The Stress Test (Finale)

**Goal:** Push the system to its limits and observe behavior.

1.  Write a **K6** script (JavaScript) that:
    * Continuously queries `GET /tickets/available` (Users checking).
    * Sends bursts of `POST /tickets/buy` (Ticket Drop).
2.  Execute the test against your local cluster (via Ingress or NodePort).

**Observation Tasks:**
1.  What happens to API latency if you disable the cache?
2.  Do messages pile up in the queue if you only have 1 worker but 100 orders/sec incoming?
3.  Scale the worker up to 5 replicas (`kubectl scale ...`) – does the queue drain faster?

**Definition of Done:**
- [ ] You used the dashboard and saw load spikes identify bottlenecks
- [ ] Services remain available even under heavy load