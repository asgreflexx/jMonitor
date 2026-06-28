// Small formatting helpers shared across the GUI.

/**
 * Derives a short, human-friendly label from a JVM's raw display name
 * (which is typically the full launch command). Keeps the simple-class-name
 * or jar file name and drops the arguments.
 */
export function shortName(displayName: string): string {
  if (!displayName || displayName === '(unknown)') return '(unknown)'
  const firstToken = displayName.trim().split(/\s+/)[0]
  if (firstToken.endsWith('.jar')) {
    return firstToken.substring(firstToken.lastIndexOf('/') + 1)
  }
  // Fully-qualified main class -> simple name
  const simple = firstToken.substring(firstToken.lastIndexOf('.') + 1)
  return simple || firstToken
}

/** Formats a millisecond duration as e.g. "3d 4h 12m" or "12m 5s". */
export function formatDuration(millis: number): string {
  const totalSeconds = Math.floor(millis / 1000)
  const d = Math.floor(totalSeconds / 86400)
  const h = Math.floor((totalSeconds % 86400) / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = totalSeconds % 60

  const parts: string[] = []
  if (d) parts.push(`${d}d`)
  if (h || d) parts.push(`${h}h`)
  if (m || h || d) parts.push(`${m}m`)
  parts.push(`${s}s`)
  return parts.slice(0, 3).join(' ')
}

/** Formats an epoch-millis timestamp as a local date-time string. */
export function formatTimestamp(epochMillis: number): string {
  return new Date(epochMillis).toLocaleString()
}

/** Formats a byte count as a human-readable string (e.g. "12.3 MB"). */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '—'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit++
  }
  return `${value.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`
}
