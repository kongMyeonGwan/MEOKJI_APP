import client from './client'

// 내 정보 · 알림 · 탈퇴 API. 로그인이 필요하다.

// 내 정보 (요리 횟수, 연속 방문일 포함)
export const getProfile = async () => {
  const response = await client.get('/api/users/me')
  return response.data
}

// 내 정보 수정. 닉네임이 겹치면 409 에러가 난다.
export const updateProfile = async (form) => {
  const response = await client.patch('/api/users/me', {
    nickname: form.nickname,
    gender: form.gender,
    address_sido: form.addressSido,
    address_sigungu: form.addressSigungu,
  })
  return response.data
}

// 비밀번호 변경
export const changeMyPassword = async (currentPassword, newPassword) => {
  const response = await client.post('/api/users/me/password', {
    current_password: currentPassword,
    new_password: newPassword,
  })
  return response.data
}

// 내 알림 (내 글에 달린 댓글, 좋아요)
export const getNotifications = async () => {
  const response = await client.get('/api/users/me/notifications')
  return response.data
}

// 회원 탈퇴. DELETE 로 본문을 보내려면 { data: ... } 에 넣어야 한다.
export const withdraw = async (password) => {
  await client.delete('/api/users/me', { data: { password } })
}
