import { useState } from 'react'
import { api } from '../api/client'
import { useAsync } from '../hooks/useAsync'
import { formatBytes, formatTimestamp } from '../util/format'

const TOP_N = 100

/** Heap class histogram + heap-dump capture/download (Phase 4). */
export function MemoryTab({ pid }: { pid: number }) {
  const histo = useAsync(pid, () => api.heapHistogram(pid))
  const dumps = useAsync(pid, () => api.heapDumps(pid))
  const [live, setLive] = useState(true)
  const [dumping, setDumping] = useState(false)

  const onDump = () => {
    setDumping(true)
    api
      .heapDump(pid, live)
      .then(() => dumps.reload())
      .catch((e: unknown) => alert(`Heap dump failed: ${e}`))
      .finally(() => setDumping(false))
  }

  return (
    <>
      <div className="toolbar">
        <button type="button" className="btn" onClick={histo.reload} disabled={histo.loading}>
          {histo.loading ? 'Loading…' : 'Refresh histogram'}
        </button>
        <span className="toolbar__sep" />
        <label className="check">
          <input type="checkbox" checked={live} onChange={(e) => setLive(e.target.checked)} />
          live only
        </label>
        <button type="button" className="btn" onClick={onDump} disabled={dumping}>
          {dumping ? 'Dumping…' : 'Heap dump'}
        </button>
      </div>

      {dumps.data && dumps.data.length > 0 && (
        <details className="panel" open>
          <summary>Heap dumps ({dumps.data.length})</summary>
          <table className="prop-table">
            <tbody>
              {dumps.data.map((d) => (
                <tr key={d.id}>
                  <td className="prop-table__key">{formatTimestamp(d.createdMillis)}</td>
                  <td className="prop-table__val">
                    {formatBytes(d.sizeBytes)} {d.live ? '· live' : '· all'}
                  </td>
                  <td className="prop-table__action">
                    <a className="btn btn--small" href={api.heapDumpDownloadUrl(d.id)}>
                      download
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      )}

      {histo.error && <div className="detail__status detail__status--error">{histo.error}</div>}

      {histo.data && (
        <>
          <div className="toolbar__info" style={{ margin: '4px 0 8px' }}>
            {histo.data.rows.length} classes · {histo.data.totalInstances.toLocaleString()} instances ·{' '}
            {formatBytes(histo.data.totalBytes)} (showing top{' '}
            {Math.min(TOP_N, histo.data.rows.length)})
          </div>
          <table className="histo-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Class</th>
                <th className="num">Instances</th>
                <th className="num">Bytes</th>
              </tr>
            </thead>
            <tbody>
              {histo.data.rows.slice(0, TOP_N).map((r) => (
                <tr key={r.rank}>
                  <td className="num">{r.rank}</td>
                  <td>
                    <code>{r.className}</code>
                  </td>
                  <td className="num">{r.instances.toLocaleString()}</td>
                  <td className="num">{formatBytes(r.bytes)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </>
  )
}
