import client from './client'

// 내 정보 · 알림 · 탈퇴 API. 전부 로그인이 필요하다.
//
// getMe() (api/auth.js) 는 토큰이 살아있는지 보는 가벼운 것이고,
// 여기 getProfile() 은 연속 방문일까지 계산해서 준다.

// 내 정보. streak_days 랑 cook_count 는 DB 컬럼이 아니라 서버가 센 값이다.
export async function getProfile() {
  const response = await client.get('/api/users/me')
  return response.data
}

// 내 정보 수정. PATCH 라서 보낸 항목만 바뀐다.
// 값이 undefined 면 axios 가 빼고 보내기 때문에 닉네임만 넘기면 주소는 그대로다.
// 닉네임이 겹치면 409 가 난다.
export async function updateProfile(form) {
  const response = await client.patch('/api/users/me', {
    nickname: form.nickname,
    gender: form.gender,
    address_sido: form.addressSido,
    address_sigungu: form.addressSigungu,
    latitude: form.latitude,
    longitude: form.longitude,
  })
  return response.data
}

// 비밀번호 변경. 비밀번호를 잊어버린 경우는 api/auth.js 쪽을 쓴다.
export async function changeMyPassword(currentPassword, newPassword) {
  const response = await client.post('/api/users/me/password', {
    current_password: currentPassword,
    new_password: newPassword,
  })
  return response.data
}

// 내 알림. 내 글에 달린 댓글이랑 좋아요를 모아서 준다.
// 알림 테이블이 따로 없어서 읽음 표시는 화면에서 처리한다.
export async function getNotifications() {
  const response = await client.get('/api/users/me/notifications')
  return response.data
}

// 회원 탈퇴.
// DELETE 인데 본문을 보내야 해서 axios 는 { data: ... } 로 넣어줘야 한다.
// 행을 지우지 않고 use_yn 을 'N' 으로 바꾸기 때문에 같은 이메일로 다시 가입된다.
export async function withdraw(password) {
  await client.delete('/api/users/me', { data: { password: password } })
}
