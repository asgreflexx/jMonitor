import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Proxy API + WebSocket calls to the Spring Boot backend during dev.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
  build: {
    // Emit the production bundle where the server build folds it into the boot
    // jar under /static (see jmonitor-server/build.gradle.kts).
    outDir: '../jmonitor-server/build/frontend-dist',
    emptyOutDir: true,
  },
})
