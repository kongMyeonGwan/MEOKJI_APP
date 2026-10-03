import client from './client'
import { authHeader } from '../utils/token'

// 자유게시판 API. 주소는 /api/free-posts 다.
//
// 보기(목록 · 상세 · 사진)는 로그인 없이도 된다. 쓰기 · 수정 · 삭제 · 댓글은
// { headers: authHeader() } 를 같이 넘겨야 한다.

// 글 목록 한 페이지. { items, total, page, size, total_pages } 가 온다.
export const getFreePosts = async (keyword, page) => {
  const response = await client.get('/api/free-posts', {
    params: { keyword: keyword, page: page },
  })
  return response.data
}

// 글 하나. 댓글도 같이 온다.
// 로그인했으면 is_mine 이 채워져서 수정 · 삭제 버튼을 보일지 알 수 있다.
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

export const addFreeComment = async (postId, content) => {
  const response = await client.post(
    `/api/free-posts/${postId}/comments`,
    { content: content },
    { headers: authHeader() },
  )
  return response.data
}

export const updateFreeComment = async (postId, commentId, content) => {
  const response = await client.put(
    `/api/free-posts/${postId}/comments/${commentId}`,
    { content: content },
    { headers: authHeader() },
  )
  return response.data
}

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

// 사진 떼기
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
