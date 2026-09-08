import client from './client'

/**
 * 내 정보 · 요리 레벨 · 알림 · 탈퇴 API.
 *
 * 전부 로그인이 필요하다. 토큰은 client.js 의 요청 인터셉터가 붙인다.
 *
 * /api/auth/me 와 무엇이 다른가
 * -----------------------------
 *   getMe()      (api/auth.js) 토큰이 살아있는지 확인. 가볍다
 *   getProfile() 요리 레벨 · 연속 방문일까지 계산해서 준다
 *
 * 새로고침할 때마다 부르는 것은 앞쪽이고, 사이드바·마이페이지가 부르는 것은
 * 뒤쪽이다.
 */

/**
 * 내 정보 + 요리 레벨.
 *
 * level · exp · exp_next · streak_days · cook_count 는 DB 컬럼이 아니라
 * 서버가 확정한 식사 · 글 · 댓글 · 받은 좋아요를 세서 계산한 값이다.
 * 퍼센트로 미리 계산해서 주지 않는다. 화면마다 필요한 표현이 다르다.
 */
export async function getProfile() {
  const { data } = await client.get('/api/users/me')
  return data
}

/**
 * 내 정보 수정. 보낸 항목만 바뀐다 (PATCH).
 *
 * undefined 인 값은 axios 가 JSON 으로 만들 때 빼주므로,
 * 닉네임만 넘기면 주소는 건드리지 않는다.
 * 닉네임이 겹치면 409 가 난다.
 */
export async function updateProfile({
  nickname,
  gender,
  addressSido,
  addressSigungu,
  latitude,
  longitude,
}) {
  const { data } = await client.patch('/api/users/me', {
    nickname,
    gender,
    address_sido: addressSido,
    address_sigungu: addressSigungu,
    latitude,
    longitude,
  })
  return data
}

/**
 * 비밀번호 변경 (로그인 상태).
 * 비밀번호를 잊어버린 경우는 api/auth.js 의 재설정 3단계를 쓴다.
 */
export async function changeMyPassword(currentPassword, newPassword) {
  const { data } = await client.post('/api/users/me/password', {
    current_password: currentPassword,
    new_password: newPassword,
  })
  return data
}

/**
 * 내 알림 — 내 글에 달린 댓글과 좋아요.
 *
 * 서버에 알림 테이블이 없다. 조회 시점에 댓글·좋아요에서 만들어 낸다.
 * 그래서 "읽음" 여부도 서버가 모르고, 화면(NotifyContext)이 판단한다.
 */
export async function getNotifications() {
  const { data } = await client.get('/api/users/me/notifications')
  return data
}

/**
 * 회원 탈퇴.
 *
 * DELETE 인데 본문을 보낸다. axios 는 두 번째 인자의 data 에 담아야 한다.
 * (POST 처럼 바로 넘기면 본문이 아니라 설정으로 해석된다.)
 *
 * 행이 지워지지 않고 use_yn='N' 이 된다. 같은 이메일로 다시 가입할 수 있다.
 */
export async function withdraw(password) {
  await client.delete('/api/users/me', { data: { password } })
}
