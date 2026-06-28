interface ChartLineProps {
  data: number[]
  /** Fixed upper bound; when omitted the chart auto-scales to the data max. */
  max?: number
  min?: number
  height?: number
  color?: string
}

/**
 * Minimal dependency-free SVG line chart for live time-series. Stretches to the
 * width of its container; the y-axis spans [min, max] (auto if max omitted).
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

  if (data.length === 0) {
    return <svg viewBox={`0 0 ${w} ${h}`} className="chart" preserveAspectRatio="none" />
  }

  const hi = max ?? Math.max(...data, 1)
  const lo = min
  const range = hi - lo || 1
  const n = data.length

  const xAt = (i: number) => (n <= 1 ? 0 : (i / (n - 1)) * w)
  const yAt = (v: number) =>
    h - ((Math.min(Math.max(v, lo), hi) - lo) / range) * (h - pad * 2) - pad

  const line = data.map((v, i) => `${xAt(i).toFixed(1)},${yAt(v).toFixed(1)}`).join(' ')
  const area = `0,${h} ${line} ${xAt(n - 1).toFixed(1)},${h}`

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
