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
}
