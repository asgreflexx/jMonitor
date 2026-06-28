// Thin fetch wrapper around the jMonitor backend API.
// In dev, requests to /api are proxied to the Spring Boot server (see
// vite.config.ts). In production the GUI is served from the same origin.

export interface HealthResponse {
  status: string
  app: string
  version: string
}

export interface ProcessInfo {
  pid: number
  displayName: string
  attachable: boolean
}

export interface JvmDetails {
  pid: number
  command: string
  vmName: string
  vmVendor: string
  vmVersion: string
  javaVersion: string
  javaHome: string
  startTimeMillis: number
  uptimeMillis: number
  inputArguments: string[]
  systemProperties: Record<string, string>
}

export interface GcStat {
  name: string
  collectionCount: number
  collectionTimeMillis: number
}

export interface MemoryPoolStat {
  name: string
  type: string
  used: number
  committed: number
  max: number
}

export interface MetricSnapshot {
  pid: number
  epochMillis: number
  heapUsed: number
  heapCommitted: number
  heapMax: number
  nonHeapUsed: number
  nonHeapCommitted: number
  processCpuLoad: number
  systemCpuLoad: number
  systemLoadAverage: number
  threadCount: number
  daemonThreadCount: number
  peakThreadCount: number
  totalStartedThreadCount: number
  loadedClassCount: number
  totalLoadedClassCount: number
  unloadedClassCount: number
  gcCount: number
  gcTimeMillis: number
  garbageCollectors: GcStat[]
  memoryPools: MemoryPoolStat[]
}

export interface MetricHistory {
  pid: number
  fromMillis: number
  toMillis: number
  stepMillis: number
  timestamps: number[]
  series: Record<string, number[]>
}

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(path, { headers: { Accept: 'application/json' } })
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}`)
  }
  return res.json() as Promise<T>
}

export const api = {
  health: () => getJson<HealthResponse>('/api/health'),
  processes: () => getJson<ProcessInfo[]>('/api/processes'),
  processDetails: (pid: number) => getJson<JvmDetails>(`/api/processes/${pid}`),
  recentMetrics: (pid: number) =>
    getJson<MetricSnapshot[]>(`/api/processes/${pid}/metrics/recent`),
  history: (pid: number, fromMillis: number, toMillis: number, metrics?: string[]) =>
    getJson<MetricHistory>(
      `/api/processes/${pid}/metrics/history?from=${fromMillis}&to=${toMillis}` +
        (metrics && metrics.length ? `&metrics=${metrics.join(',')}` : ''),
    ),
}
