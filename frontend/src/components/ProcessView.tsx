import { useEffect, useState } from 'react'
import { api, type JvmDetails } from '../api/client'
import { shortName } from '../util/format'
import { Overview } from './Overview'
import { Details } from './ProcessDetail'
import { ThreadsTab } from './ThreadsTab'
import { MBeansTab } from './MBeansTab'
import { MemoryTab } from './MemoryTab'
import { ProfilerTab } from './ProfilerTab'
import { AgentTab } from './AgentTab'

type Tab = 'overview' | 'threads' | 'mbeans' | 'memory' | 'profiler' | 'agent' | 'details'

const TABS: { key: Tab; label: string }[] = [
  { key: 'overview', label: 'Overview' },
  { key: 'threads', label: 'Threads' },
  { key: 'mbeans', label: 'MBeans' },
  { key: 'memory', label: 'Memory' },
  { key: 'profiler', label: 'Profiler' },
  { key: 'agent', label: 'Agent' },
  { key: 'details', label: 'Details' },
]

/**
 * Container for a selected JVM: connection header, tab bar, and the active tab.
 * Polls details so the header uptime stays fresh; the tabs fetch on demand.
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
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            className={'tab' + (tab === t.key ? ' tab--active' : '')}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <section className="content__body">
        {tab === 'overview' && <Overview pid={pid} />}
        {tab === 'threads' && <ThreadsTab pid={pid} />}
        {tab === 'mbeans' && <MBeansTab pid={pid} />}
        {tab === 'memory' && <MemoryTab pid={pid} />}
        {tab === 'profiler' && <ProfilerTab pid={pid} />}
        {tab === 'agent' && <AgentTab pid={pid} />}
        {tab === 'details' && <Details details={details} />}
      </section>
    </>
  )
}
