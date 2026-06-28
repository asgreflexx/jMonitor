import { useState } from 'react'
import { api } from '../api/client'
import { useAsync } from '../hooks/useAsync'
import { FlameGraph } from './FlameGraph'
import { formatBytes, formatTimestamp } from '../util/format'

/** JFR profiling: start/stop recordings and view flame graphs (Phase 5). */
export function ProfilerTab({ pid }: { pid: number }) {
  const status = useAsync(pid, () => api.jfrStatus(pid))
  const recordings = useAsync(pid, () => api.jfrRecordings(pid))
  const [profile, setProfile] = useState('profile')
  const [busy, setBusy] = useState(false)
  const [selected, setSelected] = useState<number | null>(null)

  const recording = status.data?.recording ?? false

  const onStart = () => {
    setBusy(true)
    api
      .jfrStart(pid, profile)
      .then(() => status.reload())
      .catch((e: unknown) => alert(`Start failed: ${e}`))
      .finally(() => setBusy(false))
  }

  const onStop = () => {
    setBusy(true)
    api
      .jfrStop(pid)
      .then((info) => {
        status.reload()
        recordings.reload()
        setSelected(info.id)
      })
      .catch((e: unknown) => alert(`Stop failed: ${e}`))
      .finally(() => setBusy(false))
  }

  return (
    <>
      <div className="toolbar">
        <select
          className="select"
          value={profile}
          onChange={(e) => setProfile(e.target.value)}
          disabled={recording}
        >
          <option value="profile">profile (detailed)</option>
          <option value="default">default (low overhead)</option>
        </select>
        {recording ? (
          <button type="button" className="btn" onClick={onStop} disabled={busy}>
            {busy ? 'Stopping…' : 'Stop recording'}
          </button>
        ) : (
          <button type="button" className="btn" onClick={onStart} disabled={busy}>
            {busy ? 'Starting…' : 'Start recording'}
          </button>
        )}
        {recording && (
          <span className="badge badge--ok">● recording ({status.data?.profile})</span>
        )}
      </div>

      {recordings.data && recordings.data.length > 0 && (
        <details className="panel" open>
          <summary>Recordings ({recordings.data.length})</summary>
          <table className="prop-table">
            <tbody>
              {recordings.data.map((r) => (
                <tr key={r.id}>
                  <td className="prop-table__key">{formatTimestamp(r.createdMillis)}</td>
                  <td className="prop-table__val">
                    {formatBytes(r.sizeBytes)} · {r.profile}
                  </td>
                  <td className="prop-table__action">
                    <button
                      type="button"
                      className="btn btn--small"
                      onClick={() => setSelected(r.id)}
                    >
                      flame graph
                    </button>{' '}
                    <a className="btn btn--small" href={api.jfrDownloadUrl(r.id)}>
                      download
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      )}

      {selected !== null && <FlameGraphPanel key={selected} id={selected} />}
    </>
  )
}

function FlameGraphPanel({ id }: { id: number }) {
  const { data, error, loading } = useAsync(id, () => api.jfrFlameGraph(id))
  if (loading) return <div className="detail__status">Parsing recording…</div>
  if (error) return <div className="detail__status detail__status--error">{error}</div>
  if (!data) return null
  return <FlameGraph key={id} root={data} />
}
