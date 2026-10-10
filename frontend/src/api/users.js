import client from './client'
import { authHeader } from '../utils/token'

// 내 정보 · 알림 · 탈퇴 API. 로그인이 필요하다.
//
// 주소가 /api/users/me 다. 회원 번호가 안 들어가는 이유 —
// /api/users/3 으로 두면 번호만 바꿔 남의 정보를 볼 길이 생긴다.
// "me" 는 토큰이 가리키는 사람이라 바꿔치기할 수 없다.
//
// api/auth 와 나눈 기준은 "로그인 전인가 후인가" 다. 비밀번호를 바꾸는
// 함수가 두 군데 있는 것도 그래서다 — 잊어버린 사람은 auth 의
// changePassword(이메일 인증), 알고 있는 사람은 여기 changeMyPassword.

// 내 정보 (요리 횟수, 연속 방문일 포함)
export const getProfile = async () => {
  const response = await client.get('/api/users/me', { headers: authHeader() })
  return response.data
}

// 내 정보 수정. 닉네임이 겹치면 409 에러가 난다.
//
// PUT 이 아니라 PATCH 다. 안 보낸 칸은 안 바뀐다 - 닉네임만 고치는데
// 주소까지 같이 보내야 하면 번거롭고, 빠뜨리면 주소가 지워진다.
//
// 주소를 바꾸면 서버가 좌표도 다시 찾아 저장한다. 좌표를 화면이
// 보내지 않는 이유는, 주소와 좌표가 어긋난 값이 들어갈 수 있어서다.
//
// 고친 뒤의 정보가 응답으로 온다. 화면이 그것으로 바로 다시 그려서,
// 수정 후에 getProfile 을 또 부르지 않는다
export const updateProfile = async (form) => {
  const response = await client.patch(
    '/api/users/me',
    {
      nickname: form.nickname,
      gender: form.gender,
      address_sido: form.addressSido,
      address_sigungu: form.addressSigungu,
    },
    { headers: authHeader() },
  )
  return response.data
}

// 비밀번호 변경
//
// 지금 비밀번호를 같이 보낸다. 토큰이 있으면 로그인한 사람은 맞지만,
// 자리를 비운 사이에 누가 그 브라우저를 쓸 수 있다 - 비밀번호를
// 바꾸면 계정을 넘겨주는 셈이라 한 번 더 확인한다.
//
// 바꾸고 나면 다시 로그인해야 한다. 서버가 토큰을 무효로 만들지는
// 않지만, 화면이 안내 문장을 보고 로그아웃 처리를 한다
export const changeMyPassword = async (currentPassword, newPassword) => {
  const response = await client.post(
    '/api/users/me/password',
    {
      current_password: currentPassword,
      new_password: newPassword,
    },
    { headers: authHeader() },
  )
  return response.data
}

// 내 알림 (내 글에 달린 댓글, 좋아요)
//
// 알림 표가 따로 없다. 서버가 댓글과 좋아요를 그때그때 읽어 만든다 -
// 그래서 "읽음 표시" 가 없고, 읽음 처리할 주소도 없다.
//
// 페이징이 없다. 최근 20건만 온다
export const getNotifications = async () => {
  const response = await client.get('/api/users/me/notifications', { headers: authHeader() })
  return response.data
}

// 회원 탈퇴. DELETE 로 본문을 보내려면 { data: ... } 에 넣어야 한다.
//
// POST · PUT 은 두 번째 인자가 본문인데, DELETE 는 그 자리가 없다.
// 설정 객체 안의 data 칸에 넣어야 한다 - 모르고 두 번째 인자로
// 넘기면 본문 없이 나가서 서버가 422 를 낸다.
//
// 비밀번호를 본문으로 보내는 이유 — 주소줄(?password=...)에 담으면
// 브라우저 기록과 서버 접근 로그에 그대로 남는다.
//
// 탈퇴해도 행은 지워지지 않는다. use_yn 이 'N' 으로 바뀐다 -
// 지우면 그 사람이 쓴 글 · 댓글이 가리킬 곳을 잃는다
export const withdraw = async (password) => {
  await client.delete('/api/users/me', { data: { password }, headers: authHeader() })
}
