import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173, // backend .env의 CORS_ORIGINS와 일치해야 함
  },
})
