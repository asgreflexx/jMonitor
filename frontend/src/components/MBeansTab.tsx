import { useEffect, useMemo, useState } from 'react'
import { api, type MBeanDetails } from '../api/client'

/** MBean browser: searchable object-name list + attribute/operation inspector (Phase 4). */
export function MBeansTab({ pid }: { pid: number }) {
  const [names, setNames] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<string | null>(null)

  useEffect(() => {
    setNames([])
    setSelected(null)
    api
      .mbeans(pid)
      .then((n) => {
        setNames(n)
        setError(null)
      })
      .catch((e: unknown) => setError(String(e)))
  }, [pid])

  const filtered = useMemo(
    () => names.filter((n) => n.toLowerCase().includes(query.toLowerCase())),
    [names, query],
  )

  if (error) {
    return <div className="detail__status detail__status--error">{error}</div>
  }

  return (
    <div className="mbean-layout">
      <div className="mbean-list">
        <input
          className="input"
          placeholder={`Filter ${names.length} MBeans…`}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <ul>
          {filtered.map((n) => (
            <li key={n}>
              <button
                type="button"
                className={'mbean-item' + (n === selected ? ' mbean-item--active' : '')}
                onClick={() => setSelected(n)}
                title={n}
              >
                {n}
              </button>
            </li>
          ))}
        </ul>
      </div>
      <div className="mbean-detail">
        {selected ? (
          <MBeanInspector pid={pid} name={selected} />
        ) : (
          <div className="detail__status">Select an MBean.</div>
        )}
      </div>
    </div>
  )
}

function MBeanInspector({ pid, name }: { pid: number; name: string }) {
  const [details, setDetails] = useState<MBeanDetails | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = () => {
    api
      .mbeanDetails(pid, name)
      .then((d) => {
        setDetails(d)
        setError(null)
      })
      .catch((e: unknown) => setError(String(e)))
  }

  useEffect(() => {
    setDetails(null)
    setError(null)
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pid, name])

  const onEdit = (attr: string, current: string) => {
    const next = window.prompt(`Set ${attr}`, current)
    if (next === null) return
    setBusy(true)
    api
      .setMBeanAttribute(pid, name, attr, next)
      .then(load)
      .catch((e: unknown) => alert(`Failed: ${e}`))
      .finally(() => setBusy(false))
  }

  const onInvoke = (operation: string) => {
    setBusy(true)
    api
      .invokeMBean(pid, name, operation)
      .then((r) => alert(`${operation} →\n${r.result}`))
      .catch((e: unknown) => alert(`Failed: ${e}`))
      .finally(() => setBusy(false))
  }

  if (error) return <div className="detail__status detail__status--error">{error}</div>
  if (!details) return <div className="detail__status">Loading…</div>

  return (
    <>
      <h3 className="mbean-detail__title">{details.objectName}</h3>
      <p className="mbean-detail__class">{details.className}</p>

      <h4>Attributes ({details.attributes.length})</h4>
      <table className="prop-table">
        <tbody>
          {details.attributes.map((a) => (
            <tr key={a.name}>
              <td className="prop-table__key" title={a.type}>
                {a.name}
              </td>
              <td className="prop-table__val">{a.value}</td>
              <td className="prop-table__action">
                {a.writable && (
                  <button
                    type="button"
                    className="btn btn--small"
                    disabled={busy}
                    onClick={() => onEdit(a.name, a.value)}
                  >
                    edit
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h4>Operations ({details.operations.length})</h4>
      <table className="prop-table">
        <tbody>
          {details.operations.map((op, i) => (
            <tr key={op.name + i}>
              <td className="prop-table__key">{op.returnType.split('.').pop()}</td>
              <td className="prop-table__val">
                {op.name}({op.parameterTypes.map((p) => p.split('.').pop()).join(', ')})
              </td>
              <td className="prop-table__action">
                {op.parameterTypes.length === 0 && (
                  <button
                    type="button"
                    className="btn btn--small"
                    disabled={busy}
                    onClick={() => onInvoke(op.name)}
                  >
                    invoke
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}
