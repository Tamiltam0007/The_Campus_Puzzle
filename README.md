# 🎓 Campus Scheduler — University Timetabling System

A 4-stage algorithmic scheduling pipeline built with **Java 21 + Spring Boot 3.2** (backend) and **Angular 19** (frontend).

---

## Project Structure

```
campus-scheduler/          ← Spring Boot backend
├── pom.xml
├── src/main/
│   ├── java/com/campus/scheduler/
│   │   ├── SchedulerApplication.java       Spring Boot entry point
│   │   ├── config/CorsConfig.java          CORS for Angular dev server
│   │   ├── controller/SchedulerController.java  REST API
│   │   ├── model/
│   │   │   ├── SchedulerData.java          Input DTOs (mapped from constraints.json)
│   │   │   └── ScheduleResponse.java       Output DTOs (serialised to Angular)
│   │   └── algorithm/
│   │       ├── ConflictGraph.java          Shared conflict-graph utility
│   │       ├── GreedySolver.java           Stage 1
│   │       ├── GraphEngine.java            Stage 2
│   │       ├── Optimizer.java              Stage 3
│   │       └── Backtracker.java            Stage 4
│   └── resources/
│       ├── application.properties
│       ├── constraints.json                Problem data (classes, rooms, groups)
│       └── static/                         ← Copy Angular build output here

campus-scheduler-ui/       ← Angular 19 frontend
├── src/app/
│   ├── models/schedule.ts
│   ├── services/scheduler.ts
│   └── components/
│       ├── header/
│       ├── dashboard/
│       ├── stage-pipeline/
│       ├── schedule-table/
│       ├── timetable-view/
│       ├── conflict-graph/
│       ├── comparison-chart/
│       ├── conflict-report/
│       └── algorithm-detail/
```

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+ (or use `./mvnw`)
- Node 18+ and npm (for Angular)

---

### 1. Run the Spring Boot backend

```bash
cd campus-scheduler
mvn spring-boot:run
```

API is available at `http://localhost:8080/api`

| Endpoint          | Description                        |
|-------------------|------------------------------------|
| `GET /api/health` | Health check                       |
| `GET /api/schedule` | Run all 4 stages, return report  |

---

### 2. Run the Angular frontend (dev)

```bash
cd campus-scheduler-ui
npm install
ng serve
```

Open `http://localhost:4200` — the Angular app calls `http://localhost:8080/api`.

---

### 3. Production build (optional)

Build Angular into the Spring Boot static folder so a single JAR serves everything:

```bash
cd campus-scheduler-ui
ng build --configuration=production --output-path=../campus-scheduler/src/main/resources/static

cd ../campus-scheduler
mvn package
java -jar target/scheduler-1.0.0.jar
# Visit http://localhost:8080
```

---

## Algorithm Justification

### Stage 1 — Greedy `O(C·T·R)`
Sort classes by enrollment descending (largest classes are hardest to fit — fewer rooms qualify).
Assign each to the **first** valid `(slot, room)` pair. Room selection uses **best-fit**
(smallest sufficient room) to minimise waste. Fastest stage; serves as baseline.

### Stage 2 — Welsh–Powell Graph Coloring `O(C² + C·T)`
Build a **conflict graph**: classes are nodes; an edge exists if two classes share a
professor *or* a student group. Welsh–Powell sorts nodes by degree descending and assigns
the lowest-indexed time slot not already used by any neighbour. Guarantees a conflict-free
time assignment in polynomial time.

### Stage 3 — Dynamic Programming `O(T · Cₜ · R)`
With time slots fixed by Stage 2, assign rooms to **minimise total wasted seat capacity**.
DP state: `dp[i][j]` = min waste assigning first `i` courses using first `j` rooms.
Recurrence: skip room `j`, or assign it to course `i` if capacity ≥ enrolled.
Replaces `O(R!)` brute-force with an `O(Cₜ · R)` table per slot.

### Stage 4 — Backtracking CSP `O(bᵈ)` pruned
Constraint Satisfaction with:
- **MRV** — order by conflict degree descending (most constrained first)
- **Forward Checking** — prune branches where any of the next 3 unassigned classes would have no valid option
- **Best-Effort** — if a class cannot be placed, skip it and continue; flag for manual intervention

---

## Conflict Report

Unscheduled classes (if any) are flagged with:
- Class ID and enrollment
- Reason (over-constrained, no room, no slot)
- Manual fix recommendations (split class, add slots, renegotiate rooms)

---

## Customising the Problem

Edit `src/main/resources/constraints.json` to change:
- `classes` — add/remove courses (id, name, students, professor_id)
- `rooms` — add/remove rooms (id, name, capacity)
- `time_slots` — add evening/weekend slots
- `student_groups` — define which courses each cohort must attend together
