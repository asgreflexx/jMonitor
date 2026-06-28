import { useState } from 'react'
import { Sidebar } from './components/Sidebar'
import { ProcessView } from './components/ProcessView'

function App() {
  const [selectedPid, setSelectedPid] = useState<number | null>(null)

  return (
    <div className="app">
      <Sidebar selectedPid={selectedPid} onSelect={setSelectedPid} />
      <main className="content">
        {selectedPid === null ? <Welcome /> : <ProcessView pid={selectedPid} />}
      </main>
    </div>
  )
}

function Welcome() {
  return (
    <>
      <header className="content__header">
        <h1>Overview</h1>
      </header>
      <section className="content__body">
        <div className="placeholder-card">
          <h2>Select a process</h2>
          <p>
            Pick a JVM from the list on the left to inspect its runtime details.
            The list refreshes automatically as processes start and stop.
          </p>
          <ul>
            <li>Phase 1 — Process discovery &amp; connection (Attach API) ✓</li>
            <li>Phase 2 — Live monitoring (memory, GC, threads, CPU) ✓</li>
            <li>Phase 3 — Time-series history (RRD4J) ✓</li>
            <li>Phase 4 — Thread dumps, MBean browser, heap histogram</li>
            <li>Phase 5 — JFR profiling &amp; flame graphs</li>
          </ul>
        </div>
      </section>
    </>
  )
}

export default App
