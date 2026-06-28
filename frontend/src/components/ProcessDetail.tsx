import { type JvmDetails } from '../api/client'
import { formatDuration, formatTimestamp } from '../util/format'

/**
 * "Details" tab body: static runtime information for a JVM (Phase 1 content).
 * The header/tabs are owned by {@link ProcessView}.
 */
export function Details({ details: d }: { details: JvmDetails }) {
  return (
    <>
      <div className="kv-grid">
        <KeyVal label="Java version" value={d.javaVersion} />
        <KeyVal label="VM vendor" value={d.vmVendor} />
        <KeyVal label="Uptime" value={formatDuration(d.uptimeMillis)} />
        <KeyVal label="Started" value={formatTimestamp(d.startTimeMillis)} />
        <KeyVal label="Java home" value={d.javaHome} mono />
        <KeyVal label="Command" value={d.command || '—'} mono />
      </div>

      <details className="panel">
        <summary>JVM input arguments ({d.inputArguments.length})</summary>
        {d.inputArguments.length === 0 ? (
          <p className="panel__empty">none</p>
        ) : (
          <ul className="arg-list">
            {d.inputArguments.map((a, i) => (
              <li key={i}>
                <code>{a}</code>
              </li>
            ))}
          </ul>
        )}
      </details>

      <details className="panel">
        <summary>System properties ({Object.keys(d.systemProperties).length})</summary>
        <table className="prop-table">
          <tbody>
            {Object.entries(d.systemProperties).map(([k, v]) => (
              <tr key={k}>
                <td className="prop-table__key">{k}</td>
                <td className="prop-table__val">{v}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </>
  )
}

function KeyVal({
  label,
  value,
  mono,
}: {
  label: string
  value: string
  mono?: boolean
}) {
  return (
    <div className="kv">
      <div className="kv__label">{label}</div>
      <div className={'kv__value' + (mono ? ' kv__value--mono' : '')}>{value}</div>
    </div>
  )
}
