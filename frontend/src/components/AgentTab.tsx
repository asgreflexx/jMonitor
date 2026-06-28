import { useEffect, useState } from 'react'
import { api, type AgentStatus, type MethodHotspot } from '../api/client'

const REFRESH_MS = 2000

/** Instrumentation-agent control + method hotspots (Phase 6). */
export function AgentTab({ pid }: { pid: number }) {
  const [status, setStatus] = useState<AgentStatus | null>(null)
  const [hotspots, setHotspots] = useState<MethodHotspot[]>([])
  const [prefix, setPrefix] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Poll status (so the instrumented-class count stays live) and, when loaded,
  // hotspots. Everything resets when the selected process changes.
  useEffect(() => {
    let cancelled = false
    setStatus(null)
    setHotspots([])
    setError(null)
    setPrefix('')

    const tick = async () => {
      try {
        const st = await api.agentStatus(pid)
        if (cancelled) return
        setStatus(st)
        if (st.loaded) {
          const h = await api.agentHotspots(pid)
          if (!cancelled) setHotspots(h)
        } else {
          setHotspots([])
        }
        if (!cancelled) setError(null)
      } catch (e: unknown) {
        if (!cancelled) setError(String(e))
      }
    }

    tick()
    const timer = setInterval(tick, REFRESH_MS)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [pid])

  const loaded = status?.loaded ?? false

  const onLoad = () => {
    setBusy(true)
    api
      .agentLoad(pid, prefix)
      .then((st) => {
        setStatus(st)
        setError(null)
      })
      .catch((e: unknown) => alert(`Load failed: ${e}`))
      .finally(() => setBusy(false))
  }

  const onReset = () => {
    api
      .agentReset(pid)
      .then(() => {
        setHotspots([])
        setError(null)
      })
      .catch((e: unknown) => alert(`Reset failed: ${e}`))
  }

  const maxTotal = hotspots.reduce((m, h) => Math.max(m, h.totalNanos), 1)

  return (
    <>
      <div className="toolbar">
        {!loaded ? (
          <>
            <input
              className="input"
              style={{ maxWidth: 320 }}
              placeholder="Package/class prefix to instrument (e.g. com.acme)"
              value={prefix}
              onChange={(e) => setPrefix(e.target.value)}
            />
            <button type="button" className="btn" onClick={onLoad} disabled={busy || !prefix.trim()}>
              {busy ? 'Loading…' : 'Load agent'}
            </button>
          </>
        ) : (
          <>
            <span className="badge badge--ok">
              instrumenting "{status?.prefix}" · {status?.instrumentedClassCount} classes
            </span>
            <span className="toolbar__sep" />
            <button type="button" className="btn" onClick={onReset}>
              Reset counters
            </button>
          </>
        )}
      </div>

      {!loaded && (
        <p className="panel__empty" style={{ padding: '0 2px' }}>
          Load the agent to instrument methods under a prefix and measure exact
          per-method call counts and timing. The agent attaches dynamically; it
          cannot be unloaded until the process restarts.
        </p>
      )}

      {error && <div className="detail__status detail__status--error">{error}</div>}

      {loaded && (
        <table className="histo-table">
          <thead>
            <tr>
              <th>Method</th>
              <th className="num">Calls</th>
              <th className="num">Total</th>
              <th className="num">Avg</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {hotspots.length === 0 ? (
              <tr>
                <td colSpan={5} className="panel__empty">
                  No invocations recorded yet…
                </td>
              </tr>
            ) : (
              hotspots.map((h) => (
                <tr key={h.method}>
                  <td>
                    <code>{h.method}</code>
                  </td>
                  <td className="num">{h.calls.toLocaleString()}</td>
                  <td className="num">{(h.totalNanos / 1e6).toFixed(1)} ms</td>
                  <td className="num">{(h.totalNanos / h.calls / 1e3).toFixed(1)} µs</td>
                  <td className="bar-cell">
                    <span
                      className="bar"
                      style={{ width: `${(h.totalNanos / maxTotal) * 100}%` }}
                    />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}
    </>
  )
}
