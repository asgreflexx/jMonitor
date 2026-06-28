import { useEffect, useState } from 'react'
import { api, type JvmDetails } from '../api/client'
import { formatDuration, formatTimestamp, shortName } from '../util/format'

const REFRESH_MS = 2000

type State =
  | { kind: 'loading' }
  | { kind: 'ok'; details: JvmDetails }
  | { kind: 'error'; message: string }

/** Detail panel for the currently selected JVM (Phase 1). */
export function ProcessDetail({ pid }: { pid: number }) {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [showProps, setShowProps] = useState(false)

  useEffect(() => {
    let cancelled = false
    setState({ kind: 'loading' })

    const load = () =>
      api
        .processDetails(pid)
        .then((details) => {
          if (!cancelled) setState({ kind: 'ok', details })
        })
        .catch((err: unknown) => {
          if (!cancelled) setState({ kind: 'error', message: String(err) })
        })

    load()
    // Refresh so uptime stays live; later phases stream metrics here instead.
    const timer = setInterval(load, REFRESH_MS)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [pid])

  if (state.kind === 'loading') {
    return <div className="detail__status">Connecting to JVM {pid}…</div>
  }
  if (state.kind === 'error') {
    return (
      <div className="detail__status detail__status--error">
        Could not connect to JVM {pid}: {state.message}
      </div>
    )
  }

  const d = state.details
  return (
    <div className="detail">
      <header className="content__header">
        <div>
          <h1>{shortName(d.command || d.vmName)}</h1>
          <span className="detail__sub">
            PID {d.pid} · {d.vmName} {d.vmVersion}
          </span>
        </div>
        <span className="badge badge--ok">connected</span>
      </header>

      <section className="content__body">
        <div className="kv-grid">
          <KeyVal label="Java version" value={d.javaVersion} />
          <KeyVal label="VM vendor" value={d.vmVendor} />
          <KeyVal label="Uptime" value={formatDuration(d.uptimeMillis)} />
          <KeyVal label="Started" value={formatTimestamp(d.startTimeMillis)} />
          <KeyVal label="Java home" value={d.javaHome} mono />
          <KeyVal label="Command" value={d.command || '—'} mono />
        </div>

        <details className="panel">
          <summary>JVM input arguments ({d.inputArguments.length})</summary>
          {d.inputArguments.length === 0 ? (
            <p className="panel__empty">none</p>
          ) : (
            <ul className="arg-list">
              {d.inputArguments.map((a, i) => (
                <li key={i}>
                  <code>{a}</code>
                </li>
              ))}
            </ul>
          )}
        </details>

        <details className="panel" open={showProps} onToggle={(e) => setShowProps((e.target as HTMLDetailsElement).open)}>
          <summary>
            System properties ({Object.keys(d.systemProperties).length})
          </summary>
          <table className="prop-table">
            <tbody>
              {Object.entries(d.systemProperties).map(([k, v]) => (
                <tr key={k}>
                  <td className="prop-table__key">{k}</td>
                  <td className="prop-table__val">{v}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      </section>
    </div>
  )
}

function KeyVal({
  label,
  value,
  mono,
}: {
  label: string
  value: string
  mono?: boolean
}) {
  return (
    <div className="kv">
      <div className="kv__label">{label}</div>
      <div className={'kv__value' + (mono ? ' kv__value--mono' : '')}>{value}</div>
    </div>
  )
}
