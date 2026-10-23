import client from './client'
import { authHeader } from '../utils/token'

// 회원가입 · 로그인 · 비밀번호 재설정 API

// 회원가입
export const register = async (form) => {
  const response = await client.post('/api/auth/register', {
    email: form.email,
    password: form.password,
    nickname: form.nickname,
    gender: form.gender,
    addressSido: form.addressSido,
    addressSigungu: form.addressSigungu,
  })
  return response.data
}

// 로그인. { accessToken, user } 가 온다.
export const login = async (form) => {
  const response = await client.post('/api/auth/login', {
    email: form.email,
    password: form.password,
  })
  return response.data
}

// 내 정보. 토큰이 아직 살아있는지 확인할 때도 쓴다.
export const getMe = async () => {
  const response = await client.get('/api/auth/me', { headers: authHeader() })
  return response.data
}

// 비밀번호 재설정 1단계. 이메일로 6자리 인증번호를 보낸다.
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
    newPassword: newPassword,
  })
  return response.data
}
