/**
 * 백엔드에 요청을 보내는 도구(axios 인스턴스).
 *
 * api 폴더의 모든 파일은 이 client 를 통해서만 요청한다.
 * 그래야 주소 · 타임아웃 · 토큰 · 에러 처리를 한 곳에서만 고치면 된다.
 */
import axios from 'axios'
import { getToken } from '../utils/token'

const client = axios.create({
  // .env 의 VITE_API_BASE_URL 이 있으면 그 값, 없으면 localhost:8000
  // ?? 는 "왼쪽이 null 이나 undefined 면 오른쪽을 쓴다" 는 뜻이다.
  // 이 문법과 ?. 는 프론트엔드 전체에서 쓴다.
  // 처음 본다면 docs/STUDY_JS_SYNTAX.md 를 한 번 읽고 오면 된다.
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000',
  timeout: 30000, // 30초. AI 응답이 느릴 수 있어 넉넉하게 잡았다
  headers: { 'Content-Type': 'application/json' },
})

// ==========================================================
// 요청 가로채기 — 나가기 직전에 토큰을 붙인다
//
// 인터셉터(interceptor)는 "가로채는 사람" 이라는 뜻이다.
// 요청이 서버로 나가기 직전에 이 함수를 한 번 거쳐 간다.
// 덕분에 화면 코드에서 매번 헤더를 적을 필요가 없다.
// ==========================================================
client.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    // 백엔드의 core/deps.py 가 이 헤더를 읽어서 누구인지 판단한다
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// ==========================================================
// 응답 가로채기 — 에러 모양을 하나로 통일한다
//
// 실패하는 방식이 세 가지나 되는데, 화면마다 셋을 다 구분하면 코드가 지저분해진다.
// 여기서 전부 { status, message } 모양으로 바꿔서 넘긴다.
//   1) 서버가 응답은 했는데 4xx/5xx    -> status 는 그 코드
//   2) 시간 초과                        -> status 0
//   3) 서버가 아예 응답을 안 함          -> status 0
// ==========================================================

// 토큰이 만료됐을 때 실행할 함수를 AuthContext 가 여기 등록해둔다.
// client.js 가 AuthContext 를 직접 import 하면 순환 참조가 되기 때문에,
// 반대로 AuthContext 쪽에서 "이럴 때 이 함수를 불러줘" 하고 맡기는 방식이다.
let onTokenExpired = null

export function setTokenExpiredHandler(handler) {
  onTokenExpired = handler
}

/** 요청이 성공했을 때: 손대지 않고 그대로 넘긴다 */
function onSuccess(response) {
  return response
}

/** 요청이 실패했을 때: 화면이 읽기 쉬운 모양으로 바꿔서 넘긴다 */
function onFailure(error) {
  // --- 1) 서버가 응답은 한 경우 (4xx / 5xx) ---
  if (error.response) {
    const status = error.response.status

    // 로그인 요청에서 나는 401 은 "비밀번호 틀림" 이지 세션 만료가 아니다.
    // 이걸 구분 안 하면 로그인 실패할 때마다 로그아웃 처리가 돌아간다.
    const isLoginRequest = error.config?.url?.endsWith('/api/auth/login')

    if (status === 401 && !isLoginRequest && onTokenExpired) {
      onTokenExpired()
    }

    // FastAPI 는 에러 내용을 detail 에 담아 보낸다.
    // 문자열일 때도 있고(HTTPException) 배열일 때도 있어서(422 검증 실패),
    // 문자열일 때만 쓰고 배열은 utils/apiError.js 가 raw 로 받아 처리한다.
    const detail = error.response.data?.detail
    let message = '요청을 처리하지 못했습니다.'
    if (typeof detail === 'string') {
      message = detail
    }

    return Promise.reject({
      status,
      message,
      raw: error.response.data,
    })
  }

  // --- 2) 시간 초과 ---
  if (error.code === 'ECONNABORTED') {
    return Promise.reject({
      status: 0,
      message: '요청 시간이 초과되었습니다.',
    })
  }

  // --- 3) 서버가 아예 응답하지 않음 ---
  // 백엔드를 안 켰거나, 네트워크가 끊겼거나, CORS 에 막힌 경우다.
  return Promise.reject({
    status: 0,
    message: '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.',
  })
}

client.interceptors.response.use(onSuccess, onFailure)

export default client
