import client from './client'

// 회원가입 · 로그인 · 비밀번호 재설정 API

// 회원가입. 성별과 주소는 선택이라 안 보내면 서버가 NULL 로 저장한다.
// 주소는 첫 화면의 날씨 추천에 쓴다.
export async function register(form) {
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
// user 가 같이 오기 때문에 로그인하고 getMe() 를 또 부를 필요가 없다.
export async function login(form) {
  const response = await client.post('/api/auth/login', {
    email: form.email,
    password: form.password,
  })
  return response.data
}

// 내 정보. 새로고침했을 때 토큰이 아직 살아있는지 확인하는 데도 쓴다.
export async function getMe() {
  const response = await client.get('/api/auth/me')
  return response.data
}

// --- 비밀번호 재설정 (3단계) ---

// 1단계. 이메일로 6자리 인증번호를 보낸다.
// 가입 안 된 이메일이어도 성공이라고 답한다 (가입 여부를 알려주지 않으려고).
export async function sendResetCode(email) {
  const response = await client.post('/api/auth/password-reset/send', {
    email: email,
  })
  return response.data
}

// 2단계. 인증번호 확인
export async function verifyResetCode(email, code) {
  const response = await client.post('/api/auth/password-reset/verify', {
    email: email,
    code: code,
  })
  return response.data
}

// 3단계. 새 비밀번호로 바꾸기
export async function changePassword(email, code, newPassword) {
  const response = await client.post('/api/auth/password-reset/change', {
    email: email,
    code: code,
    new_password: newPassword,
  })
  return response.data
}
