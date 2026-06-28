import { useEffect, useState } from 'react'
import { Sidebar } from './components/Sidebar'
import { api, type HealthResponse } from './api/client'

type BackendState =
  | { kind: 'loading' }
  | { kind: 'ok'; health: HealthResponse }
  | { kind: 'error'; message: string }

function App() {
  const [backend, setBackend] = useState<BackendState>({ kind: 'loading' })

  useEffect(() => {
    let cancelled = false
    api
      .health()
      .then((health) => {
        if (!cancelled) setBackend({ kind: 'ok', health })
      })
      .catch((err: unknown) => {
        if (!cancelled)
          setBackend({ kind: 'error', message: String(err) })
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="app">
      <Sidebar />
      <main className="content">
        <header className="content__header">
          <h1>Overview</h1>
          <BackendBadge state={backend} />
        </header>

        <section className="content__body">
          <div className="placeholder-card">
            <h2>Welcome to jMonitor</h2>
            <p>
              This is the Phase&nbsp;0 scaffold. The backend, frontend and build
              pipeline are wired up end to end.
            </p>
            <ul>
              <li>Phase 1 — Process discovery &amp; connection (Attach API)</li>
              <li>Phase 2 — Live monitoring (memory, GC, threads, CPU)</li>
              <li>Phase 3 — Time-series history</li>
              <li>Phase 4 — Thread dumps, MBean browser, heap histogram</li>
              <li>Phase 5 — JFR profiling &amp; flame graphs</li>
            </ul>
          </div>
        </section>
      </main>
    </div>
  )
}

function BackendBadge({ state }: { state: BackendState }) {
  if (state.kind === 'loading') {
    return <span className="badge badge--pending">Backend: connecting…</span>
  }
  if (state.kind === 'error') {
    return (
      <span className="badge badge--error" title={state.message}>
        Backend: unreachable
      </span>
    )
  }
  return (
    <span className="badge badge--ok">
      Backend: {state.health.status} · v{state.health.version}
    </span>
  )
}

export default App
