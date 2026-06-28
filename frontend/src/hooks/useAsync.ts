import { useEffect, useState } from 'react'

export interface AsyncState<T> {
  data: T | null
  error: string | null
  loading: boolean
  /** Re-runs the fetch (e.g. a manual refresh button). */
  reload: () => void
}

/**
 * Runs an async fetch keyed by `key` and exposes {data, error, loading, reload}.
 *
 * <p>Resets and re-fetches whenever `key` changes, and ignores responses from a
 * superseded key/reload (the cancellation guard), so rapidly switching the
 * selected process can never render a stale result under the new one.
 */
export function useAsync<T>(key: unknown, fn: () => Promise<T>): AsyncState<T> {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [nonce, setNonce] = useState(0)

  useEffect(() => {
    let cancelled = false
    setData(null)
    setError(null)
    setLoading(true)
    fn()
      .then((d) => {
        if (!cancelled) setData(d)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(String(e))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // fn is intentionally excluded: it is recreated each render but is keyed by
    // `key`; `nonce` drives manual reloads.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, nonce])

  return { data, error, loading, reload: () => setNonce((n) => n + 1) }
}
