import client from './client'

/**
 * 회원가입.
 * 성공 시 생성된 사용자 정보를 반환한다(비밀번호 해시는 포함되지 않음).
 *
 * 성별과 주소는 선택이다. 안 보내면 undefined 라서 axios 가 JSON 에서 빼고,
 * 서버는 NULL 로 저장한다.
 * 주소는 대시보드 첫 화면의 날씨 조회에 쓴다.
 */
export async function register({
  email,
  password,
  nickname,
  gender,
  addressSido,
  addressSigungu,
}) {
  const { data } = await client.post('/api/auth/register', {
    email,
    password,
    nickname,
    gender,
    address_sido: addressSido,
    address_sigungu: addressSigungu,
  })
  return data
}

/**
 * 로그인.
 * { access_token, token_type, expires_in, user } 를 반환한다.
 * user가 함께 오므로 로그인 직후 getMe()를 다시 부를 필요가 없다.
 */
export async function login({ email, password }) {
  const { data } = await client.post('/api/auth/login', { email, password })
  return data
}

/**
 * 내 정보 조회.
 * 토큰이 아직 살아있는지 확인하는 용도로도 쓴다 (새로고침 후 상태 복원).
 * 토큰이 없거나 만료됐으면 401이 난다.
 */
export async function getMe() {
  const { data } = await client.get('/api/auth/me')
  return data
}

// --- 비밀번호 재설정 (3단계) ---
// 계정이 잠긴 경우를 푸는 경로이기도 하다.

/**
 * 1단계. 이메일로 6자리 인증번호를 보낸다.
 * 가입되지 않은 이메일이어도 성공으로 응답한다(가입 여부를 노출하지 않기 위함).
 */
export async function sendResetCode(email) {
  const { data } = await client.post('/api/auth/password-reset/send', { email })
  return data
}

/** 2단계. 인증번호를 확인한다. */
export async function verifyResetCode(email, code) {
  const { data } = await client.post('/api/auth/password-reset/verify', {
    email,
    code,
  })
  return data
}

/** 3단계. 새 비밀번호로 교체한다. 성공하면 계정 잠금도 함께 풀린다. */
export async function changePassword(email, code, newPassword) {
  const { data } = await client.post('/api/auth/password-reset/change', {
    email,
    code,
    new_password: newPassword,
  })
  return data
}
