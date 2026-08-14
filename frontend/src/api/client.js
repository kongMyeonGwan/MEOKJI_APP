/**
 * 백엔드 API 호출용 axios 인스턴스.
 * 모든 api/*.js 는 이 client를 통해서만 요청한다.
 */
import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000',
  timeout: 30000, // AI 응답은 느릴 수 있어 넉넉히
  headers: { 'Content-Type': 'application/json' },
})

// --- 요청 인터셉터: JWT 자동 첨부 ---
// STEP 6에서 AuthContext가 setAuthToken()으로 토큰을 주입한다.
let authToken = null

export function setAuthToken(token) {
  authToken = token
}

client.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

// --- 응답 인터셉터: 에러 메시지 정규화 ---
// 어떤 실패든 화면이 읽을 수 있는 { status, message } 형태로 통일한다.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // 서버가 응답은 했지만 4xx/5xx
      const detail = error.response.data?.detail
      return Promise.reject({
        status: error.response.status,
        message:
          typeof detail === 'string' ? detail : '요청을 처리하지 못했습니다.',
        raw: error.response.data,
      })
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject({ status: 0, message: '요청 시간이 초과되었습니다.' })
    }
    // 서버가 아예 응답하지 않음 (미기동, 네트워크 단절, CORS 차단)
    return Promise.reject({
      status: 0,
      message: '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.',
    })
  }
)

export default client
