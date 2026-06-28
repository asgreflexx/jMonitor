import { useState } from 'react'
import { api, type ThreadInfoDto } from '../api/client'
import { useAsync } from '../hooks/useAsync'
import { formatTimestamp } from '../util/format'

/** Thread dump viewer with deadlock highlighting and a state filter (Phase 4). */
export function ThreadsTab({ pid }: { pid: number }) {
  const { data: dump, error, loading, reload } = useAsync(pid, () => api.threadDump(pid))
  const [filter, setFilter] = useState<string>('ALL')

  if (error) {
    return <div className="detail__status detail__status--error">{error}</div>
  }
  if (!dump) {
    return <div className="detail__status">Capturing thread dump…</div>
  }

  const states = ['ALL', ...Array.from(new Set(dump.threads.map((t) => t.state))).sort()]
  const shown = dump.threads.filter((t) => filter === 'ALL' || t.state === filter)

  return (
    <>
      <div className="toolbar">
        <button type="button" className="btn" onClick={reload} disabled={loading}>
          {loading ? 'Capturing…' : 'Recapture'}
        </button>
        <select className="select" value={filter} onChange={(e) => setFilter(e.target.value)}>
          {states.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <span className="toolbar__info">
          {dump.threads.length} threads · captured {formatTimestamp(dump.epochMillis)}
        </span>
      </div>

      {dump.deadlockedIds.length > 0 && (
        <div className="alert alert--error">
          Deadlock detected involving thread ids: {dump.deadlockedIds.join(', ')}
        </div>
      )}

      <div className="thread-list">
        {shown.map((t) => (
          <ThreadRow key={t.id} thread={t} />
        ))}
      </div>
    </>
  )
}

function ThreadRow({ thread: t }: { thread: ThreadInfoDto }) {
  return (
    <details className={'panel thread' + (t.deadlocked ? ' thread--deadlocked' : '')}>
      <summary>
        <span className={'thread__state thread__state--' + t.state}>{t.state}</span>
        <span className="thread__name">{t.name}</span>
        <span className="thread__id">#{t.id}</span>
      </summary>
      {t.lockName && (
        <div className="thread__lock">
          waiting on <code>{t.lockName}</code>
          {t.lockOwnerName ? ` held by "${t.lockOwnerName}" (#${t.lockOwnerId})` : ''}
        </div>
      )}
      {t.stackTrace.length === 0 ? (
        <p className="panel__empty">no stack frames</p>
      ) : (
        <ol className="stack">
          {t.stackTrace.map((frame, i) => (
            <li key={i}>
              <code>{frame}</code>
            </li>
          ))}
        </ol>
      )}
    </details>
  )
}
