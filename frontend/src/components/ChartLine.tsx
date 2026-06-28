interface ChartLineProps {
  data: number[]
  /** Fixed upper bound; when omitted the chart auto-scales to the data max. */
  max?: number
  min?: number
  height?: number
  color?: string
}

/**
 * Minimal dependency-free SVG line chart for time-series. Stretches to the width
 * of its container; the y-axis spans [min, max] (auto if max omitted). Non-finite
 * values (NaN gaps from the archive) are skipped, connecting across the gap.
 */
export function ChartLine({
  data,
  max,
  min = 0,
  height = 72,
  color = 'var(--accent)',
}: ChartLineProps) {
  const w = 300
  const h = height
  const pad = 3

  const n = data.length
  const finite = data.filter((v) => Number.isFinite(v))
  if (finite.length === 0) {
    return <svg viewBox={`0 0 ${w} ${h}`} className="chart" preserveAspectRatio="none" />
  }

  const hi = max ?? Math.max(...finite, 1)
  const lo = min
  const range = hi - lo || 1

  const xAt = (i: number) => (n <= 1 ? 0 : (i / (n - 1)) * w)
  const yAt = (v: number) =>
    h - ((Math.min(Math.max(v, lo), hi) - lo) / range) * (h - pad * 2) - pad

  const pts: string[] = []
  for (let i = 0; i < n; i++) {
    if (Number.isFinite(data[i])) {
      pts.push(`${xAt(i).toFixed(1)},${yAt(data[i]).toFixed(1)}`)
    }
  }
  const line = pts.join(' ')
  const firstX = pts.length ? pts[0].split(',')[0] : '0'
  const lastX = pts.length ? pts[pts.length - 1].split(',')[0] : '0'
  const area = `${firstX},${h} ${line} ${lastX},${h}`

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="chart" preserveAspectRatio="none">
      <polygon points={area} fill={color} opacity="0.12" />
      <polyline
        points={line}
        fill="none"
        stroke={color}
        strokeWidth="1.5"
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  )
}
