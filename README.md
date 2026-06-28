# jMonitor

Browser-based visualisation and monitoring for local Java processes — think
VisualVM / Dynatrace, but with a web UI.

jMonitor attaches to JVMs running on the same machine (via the JDK **Attach
API**), collects live metrics through JMX/platform MXBeans, keeps a short
time-series history, and offers diagnostics (thread dumps, MBean browser, JFR
profiling). A later phase adds a custom **java-agent** for deep instrumentation.

> **Status:** Phase 0 — scaffold. Backend, frontend and build pipeline are
> wired end to end. Process discovery and monitoring land in the next phases.

## Tech stack

| Layer      | Technology                                              |
|------------|---------------------------------------------------------|
| Backend    | Spring Boot 3.5 (Java 21), Attach API, JMX, JFR         |
| Frontend   | React + TypeScript + Vite                               |
| Build      | Gradle (Kotlin DSL), multi-module                       |
| Time-series| RRD4J (planned, Phase 3) + H2 for artifacts             |

## Modules

```
jmonitor-common   shared, dependency-free DTOs/records
jmonitor-server   Spring Boot app; also serves the built GUI from /static
frontend          React + Vite app (built into the server jar)
jmonitor-agent    java-agent for deep instrumentation (Phase 6, not yet created)
```

## Prerequisites

- A **JDK 21** (not just a JRE — the Attach API needs `jdk.attach`).
- For frontend dev: Node 18+ and npm. The Gradle build can download its own
  Node, so a system Node is only required for the live dev server.

## Running

### Production-style (single jar serves API + GUI)

```bash
# Build everything, including a fresh frontend bundle (downloads its own Node):
./gradlew build -Pfrontend

# Run it — GUI and API on http://localhost:8080
java -jar jmonitor-server/build/libs/jmonitor-server-0.1.0-SNAPSHOT.jar
```

A plain `./gradlew build` skips the frontend rebuild but still bundles an
existing `jmonitor-server/build/frontend-dist` if present.

### Development (hot-reload frontend + backend)

```bash
# Terminal 1 — backend
./gradlew :jmonitor-server:bootRun

# Terminal 2 — frontend dev server (proxies /api to :8080)
cd frontend
npm install
npm run dev          # http://localhost:5173
```

## Smoke check

```bash
curl http://localhost:8080/api/health
# {"status":"UP","app":"jMonitor","version":"0.1.0-SNAPSHOT"}
```

## Roadmap

| Phase | Scope                                                            |
|-------|-----------------------------------------------------------------|
| 0     | Scaffold (this) — build pipeline, server, GUI shell             |
| 1     | Process discovery & connection (Attach API)                     |
| 2     | Live monitoring: memory, GC, threads, CPU, classes              |
| 3     | Time-series persistence & historical charts                     |
| 4     | Thread dumps, MBean browser, heap histogram                     |
| 5     | JFR profiling & flame graphs                                     |
| 6     | java-agent: method profiling, allocation tracking               |
| 7     | Alerts, multi-process compare, packaging                        |

## License

[MIT](LICENSE) — free to fork, use, modify and distribute, including commercially.
