// 백엔드에 요청을 보내는 axios 객체.
// api 폴더의 모든 파일이 이걸 가져다 쓴다. (import client from './client')
//
// 여기서 두 가지를 자동으로 해준다.
//   1. 보내기 전  : 로그인했으면 Authorization 헤더를 붙인다
//   2. 받은 뒤    : 토큰이 만료돼 401 이 오면, 새 토큰을 받아 그 요청을 다시 보낸다
//
// 덕분에 api/*.js 의 함수들은 토큰을 전혀 몰라도 된다.

import axios from 'axios'
import { getToken, removeToken, setToken } from '../utils/token'

const client = axios.create({
  // 백엔드 주소. .env 파일의 VITE_API_BASE_URL 값을 쓰고, 없으면 스프링 서버(8090)
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090',
  // 30초 안에 응답이 없으면 실패로 본다 (레시피 만들기가 오래 걸릴 수 있어서 길게 잡았다)
  timeout: 30000,

  // 쿠키를 같이 주고받는다.
  //
  // Refresh Token 이 HttpOnly 쿠키로 오가는데, 프론트(5173)와 서버(8090)는 주소가
  // 달라서 axios 가 기본으로는 쿠키를 보내지 않는다. 이 줄이 없으면 로그인은 되지만
  // /api/auth/refresh 가 늘 401 이다 - 쿠키가 서버에 도착하지 않기 때문이다.
  //
  // 서버 쪽에도 짝이 되는 설정이 있어야 한다 (SecurityConfig 의 allowCredentials).
  withCredentials: true,
})

// ============================================================
// 1. 요청 인터셉터 — 보내기 전에 토큰 붙이기
// ============================================================
//
// 인터셉터는 "모든 요청이 지나가는 길목" 이다. 여기서 한 번 붙이면 api/*.js 의
// 함수 40여 개가 저마다 헤더를 챙기지 않아도 된다. 하나만 빠뜨려도 그 기능이
// 조용히 401 로 깨지던 것을 막는다.
client.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 로그인 안 했으면 아무것도 안 붙인다. 레시피 · 게시판 목록처럼 로그인 없이도
  // 되는 API 가 있어서, 토큰이 없다고 요청을 막으면 안 된다.

  return config
})

// ============================================================
// 2. 응답 인터셉터 — 401 이면 토큰을 새로 받아 다시 보내기
// ============================================================
//
// Access Token 은 30분이면 만료된다. 그때마다 사용자가 다시 로그인해야 한다면
// 쓸모가 없다. 그래서 401 이 오면 화면 모르게 새 토큰을 받아 원래 요청을 다시 보낸다.
//
//   글 저장 → 401 → /api/auth/refresh → 새 토큰 → 글 저장 다시 → 성공
//                                                      ↑ 사용자는 아무것도 못 느낀다

// 지금 재발급을 진행 중인가. 진행 중이면 그 Promise 가 들어 있다.
//
// 이게 왜 필요한가
//   화면 하나가 API 를 서너 개 동시에 부르는 일이 흔하다(목록 + 알림 + 내 정보).
//   토큰이 만료된 상태면 셋 다 401 을 받고, 셋 다 재발급을 부른다.
//   그런데 서버는 재발급 때마다 Refresh Token 을 새 것으로 바꾼다(회전).
//   그래서 첫 번째만 성공하고 나머지 둘은 "이미 바뀐 토큰" 으로 보이러 가서 실패하고,
//   결국 멀쩡한 사용자가 로그아웃된다.
//
//   그래서 재발급은 한 번만 하고, 나머지는 그 결과를 같이 기다리게 한다.
let refreshing = null

// 재발급 요청을 보낸다. 이미 진행 중이면 그걸 그대로 돌려준다.
const refreshAccessToken = () => {
  if (refreshing === null) {
    // 여기서 client 가 아니라 axios 를 직접 쓴다.
    // client 로 부르면 이 응답 인터셉터를 또 타게 되고, 재발급이 401 을 받았을 때
    // 다시 재발급을 부르는 무한 반복이 된다.
    refreshing = axios
      .post(
        `${client.defaults.baseURL}/api/auth/refresh`,
        null,
        { withCredentials: true }, // 쿠키를 보내야 한다. 이게 전부다.
      )
      .finally(() => {
        // 성공이든 실패든 끝났으면 자리를 비워준다.
        // 안 그러면 다음에 만료됐을 때 옛 결과를 그대로 돌려준다.
        refreshing = null
      })
  }

  return refreshing
}

client.interceptors.response.use(
  // 정상 응답은 그대로 통과시킨다
  (response) => {
    return response
  },

  // 에러 응답
  async (error) => {
    const original = error.config

    // 401 이 아니면 재발급과 무관하다. 그대로 화면으로 넘긴다.
    if (!error.response || error.response.status !== 401) {
      return Promise.reject(error)
    }

    // 로그인 · 재발급 · 로그아웃 자체가 401 인 것은 재발급으로 풀 수 없다.
    //   /login   비밀번호가 틀린 것이다
    //   /refresh 쿠키가 없거나 만료된 것이다 → 여기서 또 부르면 무한 반복
    if (
      original.url.includes('/api/auth/login') ||
      original.url.includes('/api/auth/refresh') ||
      original.url.includes('/api/auth/logout')
    ) {
      return Promise.reject(error)
    }

    // 이미 한 번 다시 보낸 요청이면 또 보내지 않는다.
    //
    // 새 토큰을 받아 다시 보냈는데도 401 이라면 토큰 문제가 아니다(탈퇴·잠김 등).
    // 이 표시가 없으면 "401 → 재발급 → 401 → 재발급 → ..." 으로 끝없이 돈다.
    if (original._retry) {
      return Promise.reject(error)
    }
    original._retry = true

    try {
      const response = await refreshAccessToken()

      // 새 Access Token 을 메모리에 넣는다. 다음 요청부터는 요청 인터셉터가
      // 알아서 이 값을 붙인다.
      setToken(response.data.access_token)

      // 원래 실패했던 요청을 다시 보낸다.
      // 이 요청의 헤더에는 아직 옛 토큰이 박혀 있으므로 직접 갈아끼운다.
      original.headers.Authorization = `Bearer ${response.data.access_token}`
      return client(original)
    } catch (refreshError) {
      // 재발급도 실패했다 = 로그아웃했거나, 14일이 지났거나, 다른 기기에서 로그인했다.
      // 더 할 수 있는 게 없으므로 메모리의 토큰을 버리고 에러를 화면으로 넘긴다.
      // (화면을 로그인 페이지로 보내는 일은 App.jsx 가 한다)
      removeToken()
      return Promise.reject(error)
    }
  },
)

export default client
