import client from './client'
import { authHeader } from '../utils/token'

// 자유게시판 API. 주소는 /api/free-posts 다.
//
// 보기(목록 · 상세 · 사진)는 로그인 없이도 된다. 쓰기 · 수정 · 삭제 · 댓글은
// { headers: authHeader() } 를 같이 넘겨야 한다.
//
// 이 파일에 글 · 댓글 · 사진이 다 들어 있어서 함수가 열두 개다.
// 주석으로 세 묶음으로 나눠 두었다.
//
// 레시피 후기(api/reviews)와 다른 점은 글을 쓰는 조건이 없다는 것뿐이다.
// 후기는 내 식사 기록을 하나 골라야 하지만 여기는 로그인만 하면 된다.
// 그리고 이쪽은 서버가 완성돼 있어서 실제로 동작한다.

// 글 목록 한 페이지. { items, total, page, size, total_pages } 가 온다.
//
// authHeader() 를 안 넘긴다. 누구나 볼 수 있고, 목록에서는 "내 글인지" 를
// 표시하지 않기 때문이다(상세에서만 표시한다).
//
// 본문은 앞 200자만 잘려서 온다. 목록에 전부 실어 보내면 응답이 무거워진다
export const getFreePosts = async (keyword, page) => {
  const response = await client.get('/api/free-posts', {
    params: { keyword: keyword, page: page },
  })
  return response.data
}

// 글 하나. 댓글도 같이 온다.
// 로그인했으면 is_mine 이 채워져서 수정 · 삭제 버튼을 보일지 알 수 있다.
//
// 로그인 안 했어도 부를 수 있다. 토큰이 없으면 authHeader() 가 빈
// 객체를 주고, 서버가 is_mine 을 false 로 채운다.
//
// 글쓴이 번호를 받아 화면이 직접 비교하지 않는 이유 — 그러려면
// 응답에 user_id 를 실어야 하는데, 화면에 쓸 일이 없는 값이다
export const getFreePost = async (postId) => {
  const response = await client.get(`/api/free-posts/${postId}`, { headers: authHeader() })
  return response.data
}

// 조회수 +1. 상세 화면에 들어올 때 한 번 부른다.
//
// 상세를 읽을 때 서버가 같이 올리지 않는다. 주소가 따로 있어서 화면이
// 올릴 시점을 정할 수 있다 - 글쓴이가 고치려고 열어도 오르면 안 되고,
// 화면이 다시 그려질 때마다 또 오르면 안 된다.
//
// GET 이 아니라 POST 다. 값을 바꾸는 요청이라 GET 으로 두면
// 브라우저가 미리 불러서 조회수가 멋대로 오른다.
//
// 로그인을 요구하지 않는다. 로그인 안 한 사람이 읽어도 조회는 조회다
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
//
// "내 글만" 을 화면이 막는 것이 아니다. 남의 글 번호로 보내면
// 서버가 403 을 낸다 - 화면이 버튼을 숨기는 것은 눈에 안 보이게
// 하는 것일 뿐이다
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
// 세 함수 모두 주소에 글 번호(postId)가 들어간다. 댓글 번호만으로도
// 찾을 수 있지만, 그러면 "3번 글의 댓글" 이라고 해놓고 실제로는 다른
// 글의 댓글을 고칠 수 있다. 서버가 둘이 맞는지 확인한다.
//
// 댓글 목록을 받는 함수가 없다. 글 상세(getFreePost)에 같이 와서
// 따로 부를 일이 없다 - 댓글이 많아도 수십 개라 한 번에 다 온다

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

// return 이 없다. 서버가 204(내용 없음)를 주기 때문에 받을 값이 없다
export const removeFreeComment = async (postId, commentId) => {
  await client.delete(`/api/free-posts/${postId}/comments/${commentId}`, { headers: authHeader() })
}

// ----- 사진 -----

// 사진 붙이기. 파일은 JSON 으로 못 보내서 FormData 로 보낸다.
// Content-Type 을 직접 적지 않는다 — 브라우저가 경계 문자열(boundary)까지
// 넣어서 알아서 붙인다. 직접 적으면 그 값이 빠져 서버가 파일을 못 읽는다.
export const uploadFreePostFile = async (postId, file) => {
  const form = new FormData()
  // 'file' 이라는 이름으로 담는다. 서버의 @RequestPart("file") 과
  // 글자가 같아야 한다 - 다르면 "파일 칸이 없다" 로 400 이 온다
  form.append('file', file)

  // 글 하나에 사진 하나다. 또 올리면 앞의 것이 바뀐다
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
// 이 함수만 async 가 아니다. 서버를 부르지 않고 글자만 이어 붙인다.
//
// 업로드 폴더 주소가 아니라 /api/free-posts/{id}/file 이다. 폴더를
// 그대로 열어두면 지워진 글의 사진도 주소만 알면 계속 보인다 -
// 이 주소를 지나면 서버가 글이 살아있는지 확인한 뒤에 읽는다
export const freePostFileUrl = (path) => {
  // 첨부가 없는 글은 path 가 null 이다. 빈 글자를 돌려주면
  // <img src=""> 가 되어 깨진 그림이 안 보인다
  if (!path) {
    return ''
  }
  // client.defaults.baseURL 을 쓰면 .env 의 주소를 그대로 따라간다.
  // 주소를 글자로 적으면 배포할 때 여기도 같이 고쳐야 한다
  return client.defaults.baseURL + path
}
