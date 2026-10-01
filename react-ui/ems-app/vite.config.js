import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxy forwards /api/** to springboot-service, so the browser
// sees same-origin requests and CORS never comes into play - avoids touching
// the existing app's CorsConfigurationSource, which is hardcoded to
// http://localhost:3000 for the sibling CRA app.
// VITE_API_PROXY_TARGET (e.g. in .env.local) overrides the springboot-service URL.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
