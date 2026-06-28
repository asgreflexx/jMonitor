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

export interface ThreadInfoDto {
  id: number
  name: string
  state: string
  stackTrace: string[]
  lockName: string | null
  lockOwnerId: number
  lockOwnerName: string | null
  blockedCount: number
  waitedCount: number
  inNative: boolean
  suspended: boolean
  deadlocked: boolean
}

export interface ThreadDump {
  pid: number
  epochMillis: number
  threads: ThreadInfoDto[]
  deadlockedIds: number[]
}

export interface MBeanAttribute {
  name: string
  type: string
  readable: boolean
  writable: boolean
  description: string
  value: string
}

export interface MBeanOperation {
  name: string
  returnType: string
  parameterTypes: string[]
  description: string
}

export interface MBeanDetails {
  objectName: string
  className: string
  description: string
  attributes: MBeanAttribute[]
  operations: MBeanOperation[]
}

export interface HeapHistogramRow {
  rank: number
  instances: number
  bytes: number
  className: string
}

export interface HeapHistogram {
  pid: number
  epochMillis: number
  totalInstances: number
  totalBytes: number
  rows: HeapHistogramRow[]
}

export interface HeapDumpInfo {
  id: number
  pid: number
  fileName: string
  sizeBytes: number
  createdMillis: number
  live: boolean
}

export interface FlameNode {
  name: string
  value: number
  children: FlameNode[]
}

export interface JfrRecordingInfo {
  id: number
  pid: number
  fileName: string
  sizeBytes: number
  createdMillis: number
  profile: string
}

export interface JfrStatus {
  recording: boolean
  profile?: string
}

export interface Alert {
  level: string
  metric: string
  message: string
  value: number
  threshold: number
}

export interface MethodHotspot {
  method: string
  calls: number
  totalNanos: number
}

export interface AgentStatus {
  loaded: boolean
  prefix: string | null
  instrumentedClassCount: number
}

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(path, { headers: { Accept: 'application/json' } })
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}`)
  }
  return res.json() as Promise<T>
}

async function postJson<T>(path: string): Promise<T> {
  const res = await fetch(path, { method: 'POST', headers: { Accept: 'application/json' } })
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
  alerts: (pid: number) => getJson<Alert[]>(`/api/processes/${pid}/alerts`),
  metricsCsvUrl: (pid: number) => `/api/processes/${pid}/metrics/export.csv`,

  // ---- Phase 4: diagnostics ----
  threadDump: (pid: number) => getJson<ThreadDump>(`/api/processes/${pid}/threaddump`),

  mbeans: (pid: number) => getJson<string[]>(`/api/processes/${pid}/mbeans`),
  mbeanDetails: (pid: number, name: string) =>
    getJson<MBeanDetails>(
      `/api/processes/${pid}/mbeans/details?name=${encodeURIComponent(name)}`,
    ),
  invokeMBean: (pid: number, name: string, operation: string) =>
    postJson<{ result: string }>(
      `/api/processes/${pid}/mbeans/invoke?name=${encodeURIComponent(name)}` +
        `&operation=${encodeURIComponent(operation)}`,
    ),
  setMBeanAttribute: async (pid: number, name: string, attribute: string, value: string) => {
    const res = await fetch(
      `/api/processes/${pid}/mbeans/attribute?name=${encodeURIComponent(name)}` +
        `&attribute=${encodeURIComponent(attribute)}&value=${encodeURIComponent(value)}`,
      { method: 'POST' },
    )
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  },

  heapHistogram: (pid: number) =>
    getJson<HeapHistogram>(`/api/processes/${pid}/heap/histogram`),
  heapDump: (pid: number, live: boolean) =>
    postJson<HeapDumpInfo>(`/api/processes/${pid}/heap/dump?live=${live}`),
  heapDumps: (pid: number) =>
    getJson<HeapDumpInfo[]>(`/api/processes/${pid}/heap/dumps`),
  heapDumpDownloadUrl: (id: number) => `/api/heap/dumps/${id}/download`,

  // ---- Phase 5: JFR profiling ----
  jfrStatus: (pid: number) => getJson<JfrStatus>(`/api/processes/${pid}/jfr/status`),
  jfrStart: (pid: number, profile: string) =>
    postJson<JfrStatus>(`/api/processes/${pid}/jfr/start?profile=${profile}`),
  jfrStop: (pid: number) => postJson<JfrRecordingInfo>(`/api/processes/${pid}/jfr/stop`),
  jfrRecordings: (pid: number) =>
    getJson<JfrRecordingInfo[]>(`/api/processes/${pid}/jfr/recordings`),
  jfrFlameGraph: (id: number) => getJson<FlameNode>(`/api/jfr/recordings/${id}/flamegraph`),
  jfrDownloadUrl: (id: number) => `/api/jfr/recordings/${id}/download`,

  // ---- Phase 6: instrumentation agent ----
  agentStatus: (pid: number) => getJson<AgentStatus>(`/api/processes/${pid}/agent/status`),
  agentLoad: (pid: number, prefix: string) =>
    postJson<AgentStatus>(`/api/processes/${pid}/agent/load?prefix=${encodeURIComponent(prefix)}`),
  agentHotspots: (pid: number) =>
    getJson<MethodHotspot[]>(`/api/processes/${pid}/agent/hotspots`),
  agentReset: async (pid: number) => {
    const res = await fetch(`/api/processes/${pid}/agent/reset`, { method: 'POST' })
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  },
}
