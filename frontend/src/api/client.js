// 백엔드에 요청을 보내는 axios 객체.
// api 폴더의 모든 파일이 이것을 가져다 쓴다.
// 주소, 토큰, 에러 처리를 여기 한 곳에만 적어두면 된다.

import axios from 'axios'
import { getToken } from '../utils/token'

// .env 에 VITE_API_BASE_URL 이 있으면 그 값을 쓰고 없으면 localhost
let baseURL = import.meta.env.VITE_API_BASE_URL
if (!baseURL) {
  baseURL = 'http://localhost:8000'
}

const client = axios.create({
  baseURL: baseURL,
  timeout: 30000, // 30초. AI 응답이 느릴 수 있어서 길게 잡았다
  headers: { 'Content-Type': 'application/json' },
})

// ---------------------------------------------
// 요청을 보내기 직전에 토큰을 헤더에 붙인다.
// 이걸 해두면 화면마다 헤더를 적지 않아도 된다.
// ---------------------------------------------
client.interceptors.request.use(function (config) {
  const token = getToken()

  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }

  return config
})

// 토큰이 만료됐을 때 실행할 함수. AuthContext 가 등록해준다.
// 여기서 AuthContext 를 직접 가져오면 서로 가져오는 문제가 생겨서 이렇게 했다.
let onTokenExpired = null

export function setTokenExpiredHandler(handler) {
  onTokenExpired = handler
}

// 성공했을 때는 그냥 넘긴다
function onSuccess(response) {
  return response
}

// 실패했을 때 에러 모양을 { status, message } 로 통일한다
function onFailure(error) {
  // 1. 서버가 응답은 했는데 4xx / 5xx 인 경우
  if (error.response) {
    const status = error.response.status

    // 로그인할 때 나는 401 은 "비밀번호 틀림" 이라서 로그아웃 처리를 하면 안 된다
    let isLoginRequest = false
    if (error.config && error.config.url) {
      isLoginRequest = error.config.url.endsWith('/api/auth/login')
    }

    if (status === 401 && !isLoginRequest && onTokenExpired) {
      onTokenExpired()
    }

    // FastAPI 는 에러 내용을 detail 에 담아준다.
    // 문자열일 때도 있고 배열일 때도 있어서 문자열일 때만 쓴다.
    let detail = null
    if (error.response.data) {
      detail = error.response.data.detail
    }

    let message = '요청을 처리하지 못했습니다.'
    if (typeof detail === 'string') {
      message = detail
    }

    // 429 일 때 몇 초 뒤에 다시 되는지 서버가 알려준다
    let retryAfter = undefined
    if (error.response.headers) {
      const seconds = Number(error.response.headers['retry-after'])
      if (Number.isFinite(seconds)) {
        retryAfter = seconds
      }
    }

    return Promise.reject({
      status: status,
      message: message,
      raw: error.response.data,
      retryAfter: retryAfter,
    })
  }

  // 2. 시간 초과
  if (error.code === 'ECONNABORTED') {
    return Promise.reject({
      status: 0,
      message: '요청 시간이 초과되었습니다.',
    })
  }

  // 3. 서버가 아예 응답하지 않음 (백엔드를 안 켰거나 네트워크 문제)
  return Promise.reject({
    status: 0,
    message: '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.',
  })
}

client.interceptors.response.use(onSuccess, onFailure)

export default client
