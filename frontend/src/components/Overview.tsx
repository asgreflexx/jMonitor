import { useEffect, useState, type ReactNode } from 'react'
import { api, type MetricSnapshot } from '../api/client'
import { subscribeMetrics } from '../ws/metricsStream'
import { ChartLine } from './ChartLine'

const MAX_POINTS = 120
const MB = 1024 * 1024

type RangeKey = 'live' | '5m' | '1h' | '1d'
const RANGES: { key: RangeKey; label: string; ms?: number }[] = [
  { key: 'live', label: 'Live' },
  { key: '5m', label: '5m', ms: 5 * 60_000 },
  { key: '1h', label: '1h', ms: 60 * 60_000 },
  { key: '1d', label: '1d', ms: 24 * 60 * 60_000 },
]

/** Common shape rendered by the charts/tiles, derived from live or archived data. */
interface ViewModel {
  heapUsedMb: number[]
  heapMaxMb: number
  cpuPct: number[]
  threads: number[]
  gcDelta: number[]
  last: {
    heapUsed: number
    heapMax: number
    cpuProcess: number
    cpuSystem: number
    threads: number
    classes: number
    gcCount: number
    gcTime: number
    nonHeapUsed: number
  }
}

const lastFinite = (a: number[]): number => {
  for (let i = a.length - 1; i >= 0; i--) if (Number.isFinite(a[i])) return a[i]
  return 0
}
const deltas = (a: number[]): number[] =>
  a.map((v, i) => (i === 0 ? 0 : Math.max(0, (v || 0) - (a[i - 1] || 0))))

/** Live overview dashboard with a time-range selector (Phase 2 live + Phase 3 history). */
export function Overview({ pid }: { pid: number }) {
  const [range, setRange] = useState<RangeKey>('live')
  const [snaps, setSnaps] = useState<MetricSnapshot[]>([])
  const [model, setModel] = useState<ViewModel | null>(null)

  // Live mode: backfill from the ring buffer, then stream over WebSocket.
  useEffect(() => {
    if (range !== 'live') return
    let cancelled = false
    setSnaps([])

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
  }, [pid, range])

  // History mode: poll the archive for the selected window.
  useEffect(() => {
    const r = RANGES.find((x) => x.key === range)
    if (!r?.ms) return
    let cancelled = false
    setModel(null)

    const load = () => {
      const to = Date.now()
      api
        .history(pid, to - r.ms!, to, [
          'heapUsed',
          'heapMax',
          'nonHeapUsed',
          'cpuProcess',
          'cpuSystem',
          'threads',
          'classes',
          'gcCount',
          'gcTime',
        ])
        .then((h) => {
          if (cancelled) return
          const s = h.series
          const get = (k: string) => s[k] ?? []
          setModel({
            heapUsedMb: get('heapUsed').map((v) => v / MB),
            heapMaxMb: (lastFinite(get('heapMax')) || Math.max(...get('heapUsed'), MB)) / MB,
            cpuPct: get('cpuProcess').map((v) => Math.max(0, v) * 100),
            threads: get('threads'),
            gcDelta: deltas(get('gcTime')),
            last: {
              heapUsed: lastFinite(get('heapUsed')),
              heapMax: lastFinite(get('heapMax')),
              cpuProcess: lastFinite(get('cpuProcess')),
              cpuSystem: lastFinite(get('cpuSystem')),
              threads: lastFinite(get('threads')),
              classes: lastFinite(get('classes')),
              gcCount: lastFinite(get('gcCount')),
              gcTime: lastFinite(get('gcTime')),
              nonHeapUsed: lastFinite(get('nonHeapUsed')),
            },
          })
        })
        .catch(() => {})
    }

    load()
    const timer = setInterval(load, 5000)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [pid, range])

  // Build the live view model from snapshots.
  const liveModel: ViewModel | null =
    range === 'live' && snaps.length > 0 ? fromSnaps(snaps) : null

  const vm = range === 'live' ? liveModel : model
  const lastSnap = range === 'live' && snaps.length ? snaps[snaps.length - 1] : null

  return (
    <>
      <div className="range-bar">
        {RANGES.map((r) => (
          <button
            key={r.key}
            type="button"
            className={'range-btn' + (range === r.key ? ' range-btn--active' : '')}
            onClick={() => setRange(r.key)}
          >
            {r.label}
          </button>
        ))}
      </div>

      {!vm ? (
        <div className="detail__status">
          {range === 'live' ? 'Waiting for live metrics…' : 'Loading history…'}
        </div>
      ) : (
        <>
          <div className="tile-grid">
            <Tile
              label="Heap"
              value={`${(vm.last.heapUsed / MB).toFixed(0)} MB`}
              sub={
                vm.last.heapMax > 0
                  ? `of ${(vm.last.heapMax / MB).toFixed(0)} MB · ${(
                      (vm.last.heapUsed / vm.last.heapMax) *
                      100
                    ).toFixed(0)}%`
                  : 'no max'
              }
            />
            <Tile
              label="Process CPU"
              value={`${(Math.max(0, vm.last.cpuProcess) * 100).toFixed(1)}%`}
              sub={`system ${(Math.max(0, vm.last.cpuSystem) * 100).toFixed(0)}%`}
            />
            <Tile
              label="Threads"
              value={`${Math.round(vm.last.threads)}`}
              sub={
                lastSnap
                  ? `peak ${lastSnap.peakThreadCount} · daemon ${lastSnap.daemonThreadCount}`
                  : 'archived average'
              }
            />
            <Tile
              label="Classes"
              value={`${Math.round(vm.last.classes)}`}
              sub={lastSnap ? `total loaded ${lastSnap.totalLoadedClassCount}` : 'loaded'}
            />
            <Tile label="GC" value={`${Math.round(vm.last.gcCount)}`} sub={`${Math.round(vm.last.gcTime)} ms total`} />
            <Tile
              label="Non-heap"
              value={`${(vm.last.nonHeapUsed / MB).toFixed(0)} MB`}
              sub={lastSnap ? `committed ${(lastSnap.nonHeapCommitted / MB).toFixed(0)} MB` : 'used'}
            />
          </div>

          <div className="chart-grid">
            <ChartCard title="Heap used (MB)" hint={`${lastFinite(vm.heapUsedMb).toFixed(0)} / ${vm.heapMaxMb.toFixed(0)}`}>
              <ChartLine data={vm.heapUsedMb} max={vm.heapMaxMb} color="var(--accent)" />
            </ChartCard>
            <ChartCard title="Process CPU (%)" hint={`${lastFinite(vm.cpuPct).toFixed(1)}%`}>
              <ChartLine data={vm.cpuPct} max={100} color="#e0883a" />
            </ChartCard>
            <ChartCard title="Threads" hint={`${Math.round(lastFinite(vm.threads))}`}>
              <ChartLine data={vm.threads} color="#52b788" />
            </ChartCard>
            <ChartCard title="GC time / interval (ms)" hint={`${Math.round(lastFinite(vm.gcDelta))} ms`}>
              <ChartLine data={vm.gcDelta} color="#c77dff" />
            </ChartCard>
          </div>

          {lastSnap && (
            <>
              <details className="panel">
                <summary>Memory pools ({lastSnap.memoryPools.length})</summary>
                <table className="prop-table">
                  <tbody>
                    {lastSnap.memoryPools.map((p) => (
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
                <summary>Garbage collectors ({lastSnap.garbageCollectors.length})</summary>
                <table className="prop-table">
                  <tbody>
                    {lastSnap.garbageCollectors.map((g) => (
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
          )}
        </>
      )}
    </>
  )
}

function fromSnaps(snaps: MetricSnapshot[]): ViewModel {
  const last = snaps[snaps.length - 1]
  const heapUsedMb = snaps.map((s) => s.heapUsed / MB)
  const heapMaxMb = last.heapMax > 0 ? last.heapMax / MB : Math.max(...heapUsedMb, 1) * 1.2
  return {
    heapUsedMb,
    heapMaxMb,
    cpuPct: snaps.map((s) => Math.max(0, s.processCpuLoad) * 100),
    threads: snaps.map((s) => s.threadCount),
    gcDelta: deltas(snaps.map((s) => s.gcTimeMillis)),
    last: {
      heapUsed: last.heapUsed,
      heapMax: last.heapMax,
      cpuProcess: last.processCpuLoad,
      cpuSystem: last.systemCpuLoad,
      threads: last.threadCount,
      classes: last.loadedClassCount,
      gcCount: last.gcCount,
      gcTime: last.gcTimeMillis,
      nonHeapUsed: last.nonHeapUsed,
    },
  }
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
