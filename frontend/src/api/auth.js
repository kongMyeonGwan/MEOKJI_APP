import client from './client'
import { authHeader } from '../utils/token'

// 회원가입 · 로그인 · 비밀번호 재설정 API
//
// 로그인 전에 쓰는 것들이라 getMe 만 빼고 토큰을 안 넘긴다.
//
// 칸 이름을 바꿔 보내는 이유
//
// 서버는 address_sido 처럼 밑줄로 받고, 리액트는 addressSido 처럼 낙타
// 모양으로 쓴다. 그 사이를 이 파일에서 바꾼다 - 화면에서 바꾸면 같은
// 변환이 화면마다 생기고, 한 곳에서 오타가 나도 그 화면만 조용히 안 된다.
//
// 보낼 칸을 하나하나 적는 이유
//
// form 을 통째로 보내면(client.post(url, form)) 화면이 들고 있는 임시
// 값까지 같이 나간다. 예를 들어 비밀번호 확인칸(passwordConfirm)은
// 서버가 받지도 않는 값이다. 적어둔 칸만 나가면 그럴 일이 없다.

// 회원가입
export const register = async (form) => {
  const response = await client.post('/api/auth/register', {
    email: form.email,
    password: form.password,
    nickname: form.nickname,
    gender: form.gender,
    address_sido: form.addressSido,
    address_sigungu: form.addressSigungu,
  })
  return response.data
}

// 로그인. { access_token, user } 가 온다.
//
// user 가 같이 오는 덕분에 로그인 직후에 /me 를 또 부르지 않아도 된다.
// 토큰을 저장하는 일은 여기서 하지 않는다 - 화면이 받아서
// utils/token 의 saveToken 을 부른다.
export const login = async (form) => {
  const response = await client.post('/api/auth/login', {
    email: form.email,
    password: form.password,
  })
  return response.data
}

// 내 정보. 토큰이 아직 살아있는지 확인할 때도 쓴다.
//
// 새로고침하면 메모리에 있던 로그인 정보가 사라진다. 토큰만
// localStorage 에 남아 있어서, 이걸 불러 다시 채운다.
// 토큰이 만료됐으면 401 이 와서 화면이 로그인 페이지로 보낸다.
export const getMe = async () => {
  const response = await client.get('/api/auth/me', { headers: authHeader() })
  return response.data
}

// 비밀번호 재설정 1단계. 이메일로 6자리 인증번호를 보낸다.
//
// 세 단계로 나뉘어 있고 그 사이에 로그인 상태가 없다. 그래서 2 · 3단계도
// 이메일을 다시 보낸다 - "누구의 비밀번호인가" 를 알려줄 길이 그것뿐이다.
//
// 가입되지 않은 이메일이면 404 가 온다. 짧은 사이에 여러 번 부르면
// 429 와 함께 Retry-After 헤더가 온다(몇 초 뒤에 되는지).
export const sendResetCode = async (email) => {
  const response = await client.post('/api/auth/password-reset/send', { email })
  return response.data
}

// 비밀번호 재설정 2단계. 인증번호 확인
export const verifyResetCode = async (email, code) => {
  const response = await client.post('/api/auth/password-reset/verify', { email, code })
  return response.data
}

// 비밀번호 재설정 3단계. 새 비밀번호로 바꾸기
export const changePassword = async (email, code, newPassword) => {
  const response = await client.post('/api/auth/password-reset/change', {
    email,
    code,
    new_password: newPassword,
  })
  return response.data
}
