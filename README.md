# 🚦 Release Risk & Canary Control Plane

## 📖 Overview
The **Canary Control Plane** is a progressive deployment automation engine designed to prevent bad software releases from reaching 100% of production users. Instead of the traditional "deploy and pray" model, this system intelligently rolls out a new release to 10%, 25%, and 50% of traffic. 

At each phase, it monitors live system latency. If an anomaly is injected (e.g. latency spikes or 5xx errors), the engine instantly halts the rollout and triggers an automatic rollback, generating a **Gemini AI RCA (Root Cause Analysis)** for the engineering team.

## 🏗️ Architecture

```mermaid
graph TD
    A[Release Deployed (10% Traffic)] --> B[Spring Boot Backend Engine]
    B --> C[PostgreSQL Database]
    B --> D[Kafka / Redis (State Tracking)]
    E[Live Traffic Latency Simulation] -.-> B
    B --> F{Is Latency > Threshold?}
    F -- Yes --> G[Trigger Auto-Rollback]
    G --> H[Gemini AI RCA Generation]
    F -- No --> I[Promote to Next Phase (25%, 50%, 100%)]
    H --> J[Next.js Grafana-Style UI]
    I --> J
```

## 🚀 Core Features
1. **Progressive Rollout Engine:** Automatically manages the state machine of a release (CREATED -> CANARY_10 -> CANARY_25 -> FULL).
2. **Live Dynamic Metrics UI:** The Next.js dashboard uses Recharts to simulate a real-time Grafana observability dashboard, tracing live Canary latency versus Baseline latency.
3. **Automated Anomaly Rollback:** Simulates injecting a 5xx latency anomaly into the canary, forcing the control plane to catch it and instantly rollback.
4. **AI Root Cause Analysis:** Once a release fails, Gemini analyzes the failure phase and latency metrics to write a clear RCA summary for on-call engineers.

## 💻 Tech Stack
* **Frontend:** Next.js, React, Recharts (for live graphing)
* **Backend:** Java 17, Spring Boot, Spring Data JPA
* **AI:** Google Gemini API
* **Database & Messaging:** PostgreSQL, Kafka, Redis, Zookeeper
* **Infrastructure:** Docker Compose for auxiliary services

## 🛠️ How to Run Locally
1. Clone the repository.
2. Spin up auxiliary services (Postgres, Kafka, Redis):
   \`\`\`bash
   docker-compose up -d
   \`\`\`
3. Start the Spring Boot Backend (Port 8080):
   \`\`\`bash
   cd backend && ./mvnw spring-boot:run
   \`\`\`
4. Start the Next.js Frontend (Port 3000):
   \`\`\`bash
   cd frontend && npm run dev
   \`\`\`
