import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import type { MetricSnapshot } from '../api/client'

// Single shared STOMP connection. The GUI views one process at a time, so we
// keep a single active subscription and re-establish it automatically after a
// reconnect.

let client: Client | null = null
let active: {
  topic: string
  cb: (s: MetricSnapshot) => void
  sub?: StompSubscription
} | null = null

function brokerUrl(): string {
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${proto}://${window.location.host}/ws`
}

function ensureClient(): Client {
  if (client) return client
  client = new Client({
    brokerURL: brokerUrl(),
    reconnectDelay: 2000,
    onConnect: () => {
      if (active) {
        active.sub = client!.subscribe(active.topic, (m: IMessage) =>
          active!.cb(JSON.parse(m.body) as MetricSnapshot),
        )
      }
    },
  })
  client.activate()
  return client
}

/**
 * Subscribes to live metrics for a pid. Replaces any previous subscription.
 * Returns an unsubscribe function.
 */
export function subscribeMetrics(
  pid: number,
  cb: (s: MetricSnapshot) => void,
): () => void {
  const c = ensureClient()
  if (active?.sub) active.sub.unsubscribe()

  active = { topic: `/topic/metrics/${pid}`, cb }
  if (c.connected) {
    active.sub = c.subscribe(active.topic, (m: IMessage) =>
      cb(JSON.parse(m.body) as MetricSnapshot),
    )
  }

  return () => {
    if (active?.sub) active.sub.unsubscribe()
    active = null
  }
}
