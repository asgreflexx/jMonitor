import { useCallback, useEffect, useState } from 'react'
import { api, type HeapDumpInfo, type HeapHistogram } from '../api/client'
import { formatBytes, formatTimestamp } from '../util/format'

const TOP_N = 100

/** Heap class histogram + heap-dump capture/download (Phase 4). */
export function MemoryTab({ pid }: { pid: number }) {
  const [histo, setHisto] = useState<HeapHistogram | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [dumps, setDumps] = useState<HeapDumpInfo[]>([])
  const [live, setLive] = useState(true)
  const [dumping, setDumping] = useState(false)

  const loadHisto = useCallback(() => {
    setLoading(true)
    api
      .heapHistogram(pid)
      .then((h) => {
        setHisto(h)
        setError(null)
      })
      .catch((e: unknown) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [pid])

  const loadDumps = useCallback(() => {
    api.heapDumps(pid).then(setDumps).catch(() => {})
  }, [pid])

  useEffect(() => {
    setHisto(null)
    setError(null)
    loadHisto()
    loadDumps()
  }, [pid, loadHisto, loadDumps])

  const onDump = () => {
    setDumping(true)
    api
      .heapDump(pid, live)
      .then(() => loadDumps())
      .catch((e: unknown) => alert(`Heap dump failed: ${e}`))
      .finally(() => setDumping(false))
  }

  return (
    <>
      <div className="toolbar">
        <button type="button" className="btn" onClick={loadHisto} disabled={loading}>
          {loading ? 'Loading…' : 'Refresh histogram'}
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

      {dumps.length > 0 && (
        <details className="panel" open>
          <summary>Heap dumps ({dumps.length})</summary>
          <table className="prop-table">
            <tbody>
              {dumps.map((d) => (
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

      {error && <div className="detail__status detail__status--error">{error}</div>}

      {histo && (
        <>
          <div className="toolbar__info" style={{ margin: '4px 0 8px' }}>
            {histo.rows.length} classes · {histo.totalInstances.toLocaleString()} instances ·{' '}
            {formatBytes(histo.totalBytes)} (showing top {Math.min(TOP_N, histo.rows.length)})
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
              {histo.rows.slice(0, TOP_N).map((r) => (
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
