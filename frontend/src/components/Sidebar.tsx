import { useEffect, useState } from 'react'
import { api, type ProcessInfo } from '../api/client'
import { shortName } from '../util/format'

const REFRESH_MS = 3000

interface SidebarProps {
  selectedPid: number | null
  onSelect: (pid: number) => void
}

/**
 * Left navigation rail showing the live list of discovered JVMs.
 * Polls /api/processes every few seconds and reflects process start/exit.
 */
export function Sidebar({ selectedPid, onSelect }: SidebarProps) {
  const [processes, setProcesses] = useState<ProcessInfo[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const load = () =>
      api
        .processes()
        .then((list) => {
          if (cancelled) return
          setProcesses(list)
          setError(null)
        })
        .catch((err: unknown) => {
          if (!cancelled) setError(String(err))
        })

    load()
    const timer = setInterval(load, REFRESH_MS)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [])

  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <span className="sidebar__logo">jM</span>
        <span className="sidebar__title">jMonitor</span>
      </div>

      <nav className="sidebar__nav">
        <div className="sidebar__section">
          Processes{processes ? ` (${processes.length})` : ''}
        </div>

        {error && <p className="sidebar__empty sidebar__empty--error">{error}</p>}
        {!processes && !error && <p className="sidebar__empty">Loading…</p>}
        {processes?.length === 0 && (
          <p className="sidebar__empty">No JVMs discovered.</p>
        )}

        <ul className="proc-list">
          {processes?.map((p) => (
            <li key={p.pid}>
              <button
                type="button"
                data-pid={p.pid}
                className={
                  'proc-item' + (p.pid === selectedPid ? ' proc-item--active' : '')
                }
                disabled={!p.attachable}
                title={p.attachable ? p.displayName : 'Cannot attach (jMonitor itself)'}
                onClick={() => p.attachable && onSelect(p.pid)}
              >
                <span className="proc-item__name">{shortName(p.displayName)}</span>
                <span className="proc-item__pid">{p.pid}</span>
              </button>
            </li>
          ))}
        </ul>
      </nav>

      <div className="sidebar__footer">v0.1.0 · Phase 3</div>
    </aside>
  )
}
