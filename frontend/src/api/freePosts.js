import client from './client'
import { authHeader } from '../utils/token'

// 자유게시판 API. 주소는 /api/free-posts 다.
//
// 보기(목록 · 상세 · 사진)는 로그인 없이도 된다. 쓰기 · 수정 · 삭제 · 댓글은
// { headers: authHeader() } 를 같이 넘겨야 한다.

// 글 목록 한 페이지. { items, total, page, size, total_pages } 가 온다.
//
// 토큰을 안 보낸다. 목록에는 수정 · 삭제 버튼이 없어서 "내 글인지" 를 알
// 필요가 없고, 그래서 items 안에 is_mine 칸도 없다.
export const getFreePosts = async (keyword, page) => {
  const response = await client.get('/api/free-posts', {
    params: { keyword: keyword, page: page },
  })
  return response.data
}

// 글 하나. 댓글도 같이 온다.
//
// 목록과 달리 토큰을 같이 보낸다. 상세는 로그인 없이도 보이지만, 토큰이
// 있어야 서버가 is_mine 을 채워준다. 토큰 없이 부르면 is_mine 이 전부 false 로
// 와서 내가 쓴 글인데도 수정 · 삭제 버튼이 안 보인다.
//
// is_mine 은 두 군데에 따로 온다
//   data.is_mine              이 글을 내가 썼는지     → 글 수정 · 삭제 · 사진 버튼
//   data.comments[i].is_mine  그 댓글을 내가 썼는지   → 그 댓글의 수정 · 삭제 버튼
//
// 남의 글에도 내가 댓글을 달 수 있으니 둘은 서로 상관이 없다. 글은 false 인데
// 그 안의 내 댓글만 true 인 경우가 가장 흔하다. 그래서 하나로 합쳐 쓸 수 없고,
// 글 버튼은 data.is_mine, 댓글 버튼은 그 댓글의 is_mine 을 봐야 한다.
export const getFreePost = async (postId) => {
  const response = await client.get(`/api/free-posts/${postId}`, { headers: authHeader() })
  return response.data
}

// 조회수 +1. 상세 화면에 들어올 때 한 번 부른다.
export const countFreePostView = async (postId) => {
  const response = await client.post(`/api/free-posts/${postId}/view`)
  return response.data
}

// 글쓰기
export const createFreePost = async (form) => {
  const response = await client.post(
    '/api/free-posts',
    { title: form.title, content: form.content },
    { headers: authHeader() },
  )
  return response.data
}

// 글 수정 (내 글만)
export const updateFreePost = async (postId, form) => {
  const response = await client.put(
    `/api/free-posts/${postId}`,
    { title: form.title, content: form.content },
    { headers: authHeader() },
  )
  return response.data
}

// 글 삭제 (내 글만)
export const removeFreePost = async (postId) => {
  await client.delete(`/api/free-posts/${postId}`, { headers: authHeader() })
}

// ----- 댓글 -----
//
// 셋 다 토큰을 보낸다. 누가 쓴 댓글인지는 서버가 토큰에서 꺼내므로 본문으로
// 보내지 않는다 — 보낼 수 있으면 남의 이름으로 댓글을 달 수 있다.
// 수정 · 삭제는 서버가 "그 댓글을 쓴 사람" 인지 다시 보고, 아니면 403 을 준다.
// 화면에서 버튼을 숨기는 것만으로는 막는 것이 아니다.

// 댓글 쓰기. 돌려받은 댓글에는 is_mine 이 true 로 들어 있다
export const addFreeComment = async (postId, content) => {
  const response = await client.post(
    `/api/free-posts/${postId}/comments`,
    { content: content },
    { headers: authHeader() },
  )
  return response.data
}

// 댓글 수정 (내 댓글만). 주소에 글 번호와 댓글 번호가 둘 다 들어간다 —
// 서버가 그 댓글이 정말 이 글의 댓글인지까지 확인한다
export const updateFreeComment = async (postId, commentId, content) => {
  const response = await client.put(
    `/api/free-posts/${postId}/comments/${commentId}`,
    { content: content },
    { headers: authHeader() },
  )
  return response.data
}

// 댓글 삭제 (내 댓글만). 돌려주는 내용이 없다(204)
export const removeFreeComment = async (postId, commentId) => {
  await client.delete(`/api/free-posts/${postId}/comments/${commentId}`, { headers: authHeader() })
}

// ----- 사진 -----

// 사진 붙이기. 파일은 JSON 으로 못 보내서 FormData 로 보낸다.
// Content-Type 을 직접 적지 않는다 — 브라우저가 경계 문자열(boundary)까지
// 넣어서 알아서 붙인다. 직접 적으면 그 값이 빠져 서버가 파일을 못 읽는다.
export const uploadFreePostFile = async (postId, file) => {
  const form = new FormData()
  form.append('file', file)

  const response = await client.post(`/api/free-posts/${postId}/file`, form, {
    headers: authHeader(),
  })
  return response.data
}

// 사진 떼기 (글쓴이만)
export const removeFreePostFile = async (postId) => {
  await client.delete(`/api/free-posts/${postId}/file`, { headers: authHeader() })
}

// 사진 주소 만들기.
// 서버는 '/api/free-posts/12/file' 처럼 앞에 서버 주소가 없는 값을 준다.
// <img src> 에 그대로 넣으면 리액트 주소(5173)로 찾아가므로 서버 주소를 붙인다.
export const freePostFileUrl = (path) => {
  if (!path) {
    return ''
  }
  return client.defaults.baseURL + path
}
