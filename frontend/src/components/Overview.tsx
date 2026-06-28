import { useEffect, useState, type ReactNode } from 'react'
import { api, type MetricSnapshot } from '../api/client'
import { subscribeMetrics } from '../ws/metricsStream'
import { ChartLine } from './ChartLine'

const MAX_POINTS = 120
const MB = 1024 * 1024

/** Live overview dashboard for a JVM: metric tiles + streaming charts (Phase 2). */
export function Overview({ pid }: { pid: number }) {
  const [snaps, setSnaps] = useState<MetricSnapshot[]>([])

  useEffect(() => {
    let cancelled = false
    setSnaps([])

    // Backfill from the in-memory ring buffer, then stream live.
    api
      .recentMetrics(pid)
      .then((recent) => {
        if (!cancelled) setSnaps(recent.slice(-MAX_POINTS))
      })
      .catch(() => {})

    const unsubscribe = subscribeMetrics(pid, (s) => {
      if (cancelled) return
      setSnaps((prev) => {
        const next = [...prev, s]
        return next.length > MAX_POINTS ? next.slice(next.length - MAX_POINTS) : next
      })
    })

    return () => {
      cancelled = true
      unsubscribe()
    }
  }, [pid])

  if (snaps.length === 0) {
    return <div className="detail__status">Waiting for live metrics…</div>
  }

  const last = snaps[snaps.length - 1]
  const heapUsedMb = snaps.map((s) => s.heapUsed / MB)
  const heapMaxMb =
    last.heapMax > 0 ? last.heapMax / MB : Math.max(...heapUsedMb, 1) * 1.2
  const cpu = snaps.map((s) => Math.max(0, s.processCpuLoad) * 100)
  const threads = snaps.map((s) => s.threadCount)
  const gcDelta = snaps.map((s, i) =>
    i === 0 ? 0 : Math.max(0, s.gcTimeMillis - snaps[i - 1].gcTimeMillis),
  )

  const heapPct = last.heapMax > 0 ? (last.heapUsed / last.heapMax) * 100 : null

  return (
    <>
      <div className="tile-grid">
        <Tile
          label="Heap"
          value={`${(last.heapUsed / MB).toFixed(0)} MB`}
          sub={
            last.heapMax > 0
              ? `of ${(last.heapMax / MB).toFixed(0)} MB · ${heapPct!.toFixed(0)}%`
              : 'no max'
          }
        />
        <Tile
          label="Process CPU"
          value={`${(Math.max(0, last.processCpuLoad) * 100).toFixed(1)}%`}
          sub={`system ${(Math.max(0, last.systemCpuLoad) * 100).toFixed(0)}%`}
        />
        <Tile
          label="Threads"
          value={`${last.threadCount}`}
          sub={`peak ${last.peakThreadCount} · daemon ${last.daemonThreadCount}`}
        />
        <Tile
          label="Classes"
          value={`${last.loadedClassCount}`}
          sub={`total loaded ${last.totalLoadedClassCount}`}
        />
        <Tile
          label="GC"
          value={`${last.gcCount}`}
          sub={`${last.gcTimeMillis} ms total`}
        />
        <Tile
          label="Non-heap"
          value={`${(last.nonHeapUsed / MB).toFixed(0)} MB`}
          sub={`committed ${(last.nonHeapCommitted / MB).toFixed(0)} MB`}
        />
      </div>

      <div className="chart-grid">
        <ChartCard title="Heap used (MB)" hint={`${heapUsedMb.at(-1)!.toFixed(0)} / ${heapMaxMb.toFixed(0)}`}>
          <ChartLine data={heapUsedMb} max={heapMaxMb} color="var(--accent)" />
        </ChartCard>
        <ChartCard title="Process CPU (%)" hint={`${cpu.at(-1)!.toFixed(1)}%`}>
          <ChartLine data={cpu} max={100} color="#e0883a" />
        </ChartCard>
        <ChartCard title="Threads" hint={`${threads.at(-1)}`}>
          <ChartLine data={threads} color="#52b788" />
        </ChartCard>
        <ChartCard title="GC time / interval (ms)" hint={`${gcDelta.at(-1)} ms`}>
          <ChartLine data={gcDelta} color="#c77dff" />
        </ChartCard>
      </div>

      <details className="panel">
        <summary>Memory pools ({last.memoryPools.length})</summary>
        <table className="prop-table">
          <tbody>
            {last.memoryPools.map((p) => (
              <tr key={p.name}>
                <td className="prop-table__key">{p.name}</td>
                <td className="prop-table__val">
                  {(p.used / MB).toFixed(1)} MB used ·{' '}
                  {p.max > 0
                    ? `${(p.max / MB).toFixed(0)} MB max`
                    : `${(p.committed / MB).toFixed(0)} MB committed`}{' '}
                  ({p.type})
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>

      <details className="panel">
        <summary>Garbage collectors ({last.garbageCollectors.length})</summary>
        <table className="prop-table">
          <tbody>
            {last.garbageCollectors.map((g) => (
              <tr key={g.name}>
                <td className="prop-table__key">{g.name}</td>
                <td className="prop-table__val">
                  {g.collectionCount} collections · {g.collectionTimeMillis} ms
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </>
  )
}

function Tile({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="tile">
      <div className="tile__label">{label}</div>
      <div className="tile__value">{value}</div>
      {sub && <div className="tile__sub">{sub}</div>}
    </div>
  )
}

function ChartCard({
  title,
  hint,
  children,
}: {
  title: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="chart-card">
      <div className="chart-card__head">
        <span>{title}</span>
        {hint && <span className="chart-card__hint">{hint}</span>}
      </div>
      {children}
    </div>
  )
}
