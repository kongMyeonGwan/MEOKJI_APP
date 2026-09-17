import client from './client'

// 게시판 API.
// 목록이랑 상세는 로그인 안 해도 볼 수 있다.
// 로그인했으면 서버가 liked / is_mine 을 계산해서 같이 준다.

// 게시글 목록 한 페이지. 최신순.
// 배열이 아니라 { items, total, page, size, total_pages } 가 온다.
// 화면에서 "3 / 7" 을 그리려면 전체가 몇 건인지 알아야 해서 이렇게 온다.
// page 는 1부터다.
export async function getPosts(keyword, page) {
  let pageNumber = page
  if (!pageNumber) {
    pageNumber = 1
  }

  const params = { page: pageNumber }

  // 검색어가 비어 있으면 아예 안 보낸다
  if (keyword) {
    const trimmed = keyword.trim()
    if (trimmed) {
      params.keyword = trimmed
    }
  }

  const response = await client.get('/api/posts', { params: params })
  return response.data
}

// 게시글 상세 + 댓글. 없는 번호면 404 가 난다.
export async function getPost(postId) {
  const response = await client.get('/api/posts/' + postId)
  return response.data
}

// 글쓰기. 확정한 식사 기록 하나를 골라야 쓸 수 있다.
// 레시피랑 재료는 서버가 원본 기록에서 복사해온다.
// 이미 공유한 기록이면 409 가 난다.
export async function createPost(form) {
  const response = await client.post('/api/posts', {
    meal_history_id: form.mealHistoryId,
    title: form.title,
    content: form.content,
  })
  return response.data
}

// 좋아요 켜기 / 끄기. { liked, like_count } 가 온다.
// 토글이 아니라 원하는 상태를 보낸다. 그래야 따닥 눌러도 숫자가 안 어긋난다.
export async function setPostLike(postId, liked) {
  let response = null

  if (liked) {
    response = await client.put('/api/posts/' + postId + '/like')
  } else {
    response = await client.delete('/api/posts/' + postId + '/like')
  }

  return response.data
}

// 댓글 작성
export async function addComment(postId, content) {
  const response = await client.post('/api/posts/' + postId + '/comments', {
    content: content,
  })
  return response.data
}

// 글 수정. 제목이랑 본문만 바뀐다.
// 어느 기록에 대한 글인지는 못 바꾼다. 그게 바뀌면 다른 글이 되기 때문이다.
export async function updatePost(postId, form) {
  const response = await client.put('/api/posts/' + postId, {
    title: form.title,
    content: form.content,
  })
  return response.data
}

// 글 삭제. 내 글만 지울 수 있다.
export async function removePost(postId) {
  await client.delete('/api/posts/' + postId)
}

// 사진 올리기 / 바꾸기. 글 하나당 사진 하나라서 또 올리면 갈아 끼워진다.
//
// 글쓰기랑 따로 부른다. 한 요청에 JSON 이랑 파일을 같이 담을 수 없어서다.
// 이게 실패해도 글은 이미 올라가 있다.
//
// Content-Type 을 여기서 다시 적어줘야 한다. client.js 기본값이 json 이라
// 그냥 두면 axios 가 파일을 버리고 문자열로 보내버린다.
export async function uploadPostFile(postId, file) {
  const form = new FormData()
  form.append('file', file)

  const response = await client.post('/api/posts/' + postId + '/file', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

// 사진 떼기. 사진이 없어도 에러는 아니다.
export async function removePostFile(postId) {
  await client.delete('/api/posts/' + postId + '/file')
}

// 사진을 실제로 가져올 주소를 만든다.
// 서버는 '/api/posts/12/file' 처럼 앞부분 없이 주기 때문에
// <img src> 에 넣으려면 백엔드 주소를 앞에 붙여야 한다.
export function postFileUrl(path) {
  if (!path) {
    return ''
  }

  let base = client.defaults.baseURL
  if (!base) {
    base = ''
  }

  return base + path
}

// 조회수 +1.
// 상세를 볼 때 서버가 같이 안 올린다. GET 은 몇 번 불러도 결과가 같아야 해서다.
export async function countPostView(postId) {
  const response = await client.post('/api/posts/' + postId + '/view')
  return response.data
}

// 댓글 수정. 내 댓글만 고칠 수 있다.
export async function updateComment(postId, commentId, content) {
  const response = await client.put(
    '/api/posts/' + postId + '/comments/' + commentId,
    { content: content },
  )
  return response.data
}

// 댓글 삭제. 내 댓글만 지울 수 있다.
export async function removeComment(postId, commentId) {
  await client.delete('/api/posts/' + postId + '/comments/' + commentId)
}

// 인기 레시피. 고른 기간에 새로 받은 좋아요가 많은 순으로 온다.
// 누적으로 세면 오래된 글이 계속 1등이라 순위가 안 움직인다.
// start / end 는 'YYYY-MM-DD' 이고, 안 주면 최근 30일이다.
export async function getPopular(options) {
  let start = undefined
  let end = undefined
  let limit = undefined

  if (options) {
    start = options.start
    end = options.end
    limit = options.limit
  }

  const response = await client.get('/api/posts/popular', {
    params: { start: start, end: end, limit: limit },
  })
  return response.data
}
