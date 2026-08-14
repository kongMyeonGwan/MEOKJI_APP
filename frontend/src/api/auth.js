import client from './client'

/**
 * 회원가입.
 * 성공 시 생성된 사용자 정보를 반환한다(비밀번호 해시는 포함되지 않음).
 */
export async function register({ email, password, nickname }) {
  const { data } = await client.post('/api/auth/register', {
    email,
    password,
    nickname,
  })
  return data
}
