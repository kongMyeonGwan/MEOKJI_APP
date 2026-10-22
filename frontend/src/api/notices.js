// 공지사항 API.
// 작성 · 수정 · 삭제는 관리자만 된다. { headers: authHeader() } 를 같이 넘긴다.
// 목록 · 상세 · 조회수는 누구나 볼 수 있어서 넘기지 않는다.

import client from './client'
import { authHeader } from '../utils/token'

// 공지 목록 한 페이지. 고정 공지가 위, 그 다음 최신순.
// options: { page, size }  →  { items, page, size, total, total_pages }
export const getNotices = async ({ page = 1, size = 20 } = {}) => {
  const res = await client.get('/api/notices', { params: { page, size } })
  return res.data
}

// 공지 하나. 상세 화면이 부른다. (조회수는 올리지 않는다)
export const getNotice = async (noticeId) => {
  const res = await client.get(`/api/notices/${noticeId}`)
  return res.data
}

// 조회수 +1. 올린 뒤의 숫자를 돌려준다.
export const countNoticeView = async (noticeId) => {
  const res = await client.post(`/api/notices/${noticeId}/view`)
  return res.data
}

// 아래는 관리자만 쓸 수 있다

// 공지 작성. form: { title, content, pinned }
export const createNotice = async (form) => {
  const res = await client.post('/api/notices', form, { headers: authHeader() })
  return res.data
}

// 공지 수정. form: { title, content, pinned }
export const updateNotice = async (noticeId, form) => {
  const res = await client.put(`/api/notices/${noticeId}`, form, { headers: authHeader() })
  return res.data
}

// 공지 삭제. 서버가 204 (돌려줄 내용 없음) 라서 값을 안 돌려준다.
export const deleteNotice = async (noticeId) => {
  await client.delete(`/api/notices/${noticeId}`, { headers: authHeader() })
}