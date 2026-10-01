import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173, // backend application.yml 의 app.cors-origins 와 일치해야 함
    // 5173 을 다른 프로그램이 쓰고 있으면 5174 로 옮기지 말고 멈춘다.
    // 옮겨 가면 CORS 에 막혀서 모든 API 가 "서버에 연결할 수 없습니다" 로 실패한다.
    strictPort: true,
  },
})
