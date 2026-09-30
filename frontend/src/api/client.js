// 백엔드에 요청을 보내는 axios 객체.
// api 폴더의 모든 파일이 이걸 가져다 쓴다. (import client from './client')

import axios from 'axios'
import { getToken, removeToken, removeUser } from '../utils/token'

// 1. 기본 설정을 넣어서 axios 객체를 만든다
const client = axios.create({
  // 백엔드 주소. .env 파일의 VITE_API_BASE_URL 값을 쓰고, 없으면 스프링 서버(8080)
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  // 30초 안에 응답이 없으면 실패로 본다 (AI 응답이 느릴 수 있어서 길게 잡았다)
  timeout: 30000,
})

// 2. 요청을 보내기 "전에" 자동으로 실행되는 함수 (interceptor)
//    로그인 토큰을 요청 헤더에 붙인다.
//    이걸 여기 한 번만 써두면 api 파일마다 헤더를 적지 않아도 된다.
//    서버는 이 헤더를 보고 "누가 보낸 요청인지" 안다.
client.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    // 모양: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// 3. 응답을 받은 "후에" 자동으로 실행되는 함수 (interceptor)
//    첫 번째 함수: 성공했을 때 → 그대로 돌려준다
//    두 번째 함수: 실패했을 때 → 로그인이 만료됐는지만 확인하고 에러를 그대로 던진다
client.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    // 상태 코드 401 = "로그인 토큰이 만료됐거나 잘못됐다" (토큰은 30분 뒤 만료된다)
    const is401 = error.response && error.response.status === 401

    // 로그인 화면에서 비밀번호를 틀린 것도 401 이다. 그건 만료가 아니니까 뺀다.
    const isLoginRequest = error.config.url === '/api/auth/login'

    // getToken() 을 같이 보는 이유: 요청 여러 개가 동시에 401 을 받아도
    // 알림창은 한 번만 뜨게 하려고 (첫 번째에서 토큰을 지우니까 두 번째부터는 건너뛴다)
    if (is401 && !isLoginRequest && getToken()) {
      removeToken()
      removeUser()
      alert('로그인이 만료되었습니다. 다시 로그인해주세요.')
      window.location.href = '/login' // 페이지를 새로 불러오면서 로그인 화면으로 이동
    }

    // 에러를 그대로 던진다 → 화면의 try/catch 에서 catch (err) 로 받는다
    return Promise.reject(error)
  },
)

export default client
