import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxy forwards /api/** to springboot-service, so the browser
// sees same-origin requests and CORS never comes into play - avoids touching
// the existing app's CorsConfigurationSource, which is hardcoded to
// http://localhost:3000 for the sibling CRA app.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
