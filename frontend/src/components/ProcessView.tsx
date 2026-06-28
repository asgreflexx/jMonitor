import { useEffect, useState } from 'react'
import { api, type JvmDetails } from '../api/client'
import { shortName } from '../util/format'
import { Overview } from './Overview'
import { Details } from './ProcessDetail'

type Tab = 'overview' | 'details'

/**
 * Container for a selected JVM: connection header, tab bar, and the active tab
 * (live Overview or static Details). Polls details so the header uptime stays
 * fresh; live metrics flow through the Overview's WebSocket stream.
 */
export function ProcessView({ pid }: { pid: number }) {
  const [details, setDetails] = useState<JvmDetails | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [tab, setTab] = useState<Tab>('overview')

  useEffect(() => {
    let cancelled = false
    setDetails(null)
    setError(null)
    setTab('overview')

    const load = () =>
      api
        .processDetails(pid)
        .then((d) => {
          if (!cancelled) {
            setDetails(d)
            setError(null)
          }
        })
        .catch((e: unknown) => {
          if (!cancelled) setError(String(e))
        })

    load()
    const timer = setInterval(load, 2000)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [pid])

  if (error) {
    return (
      <div className="detail__status detail__status--error">
        Could not connect to JVM {pid}: {error}
      </div>
    )
  }
  if (!details) {
    return <div className="detail__status">Connecting to JVM {pid}…</div>
  }

  return (
    <>
      <header className="content__header">
        <div>
          <h1>{shortName(details.command || details.vmName)}</h1>
          <span className="detail__sub">
            PID {details.pid} · {details.vmName} {details.vmVersion}
          </span>
        </div>
        <span className="badge badge--ok">connected</span>
      </header>

      <div className="tabs">
        <button
          type="button"
          className={'tab' + (tab === 'overview' ? ' tab--active' : '')}
          onClick={() => setTab('overview')}
        >
          Overview
        </button>
        <button
          type="button"
          className={'tab' + (tab === 'details' ? ' tab--active' : '')}
          onClick={() => setTab('details')}
        >
          Details
        </button>
      </div>

      <section className="content__body">
        {tab === 'overview' ? <Overview pid={pid} /> : <Details details={details} />}
      </section>
    </>
  )
}
