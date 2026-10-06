import client from './client'

// 회원가입 · 로그인 · 비밀번호 재설정 API

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

// 로그인. { access_token, token_type, expires_in, user } 가 온다.
//
// Refresh Token 은 응답에 없다. 서버가 HttpOnly 쿠키로 따로 심어주고, 그 쿠키는
// 자바스크립트가 읽을 수 없다. 우리가 할 일도 없다 - 브라우저가 알아서 들고 있다가
// /api/auth 로 요청을 보낼 때 붙여준다.
//
// keep 은 화면의 '로그인 상태 유지' 체크다. 이 값에 따라 쿠키 수명이 갈린다.
//   true  → 14일. 브라우저를 껐다 켜도 로그인이 복원된다.
//   false → 브라우저를 닫으면 사라진다 (공용 PC)
export const login = async (form) => {
  const response = await client.post('/api/auth/login', {
    email: form.email,
    password: form.password,
    keep: form.keep,
  })
  return response.data
}

// Access Token 다시 받기. 응답 모양은 login 과 같다.
//
// 보내는 게 아무것도 없다. 쿠키에 든 Refresh Token 만으로 서버가 판단한다.
// 앱을 켤 때 한 번 불러서 로그인 상태를 되살리는 데 쓴다 (App.jsx).
//
// 30분이 지나 API 가 401 을 냈을 때도 불리는데, 그건 client.js 의 응답
// 인터셉터가 알아서 하므로 화면에서 신경 쓸 필요가 없다.
export const refresh = async () => {
  const response = await client.post('/api/auth/refresh')
  return response.data
}

// 로그아웃. 서버가 저장해둔 Refresh Token 을 지우고 쿠키도 지운다.
//
// 화면에서 그냥 토큰만 버리면 안 된다. 그러면 쿠키가 그대로 남아서, 다음에
// 앱을 켤 때 refresh 가 성공해버린다 - 로그아웃한 것처럼 보였다가 되살아난다.
export const logout = async () => {
  const response = await client.post('/api/auth/logout')
  return response.data
}

// 내 정보. 토큰이 아직 살아있는지 확인할 때도 쓴다.
export const getMe = async () => {
  const response = await client.get('/api/auth/me')
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
    new_password: newPassword,
  })
  return response.data
}
