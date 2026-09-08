import client from './client'

/**
 * 게시판 API (STEP 20~23).
 *
 * 조회는 Guest도 가능하다. 토큰이 있으면 client.js가 알아서 붙이고,
 * 서버는 그때만 liked / is_mine 을 계산해 내려준다.
 */

/**
 * 게시글 목록. 최신순으로 온다.
 *
 * content 는 목록용으로 잘린 미리보기이고, 레시피(recipe_json)는 들어있지 않다.
 * 목록에서 쓰지 않는 CLOB을 건당 읽어오지 않기 위해서다.
 */
export async function getPosts(keyword) {
  const { data } = await client.get('/api/posts', {
    params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
  })
  return data
}

/** 게시글 상세 + 댓글. 없는 id면 404가 난다. */
export async function getPost(postId) {
  const { data } = await client.get(`/api/posts/${postId}`)
  return data
}

/**
 * 글쓰기. 확정한 식사(meal_history_id) 하나에서만 쓸 수 있다.
 *
 * 레시피/재료는 보내지 않는다. 서버가 원본 기록에서 직접 복사한다.
 * 이미 공유한 기록이면 409가 난다.
 */
export async function createPost({ mealHistoryId, title, content }) {
  const { data } = await client.post('/api/posts', {
    meal_history_id: mealHistoryId,
    title,
    content,
  })
  return data
}

/**
 * 좋아요 켜기/끄기. { liked, like_count } 를 돌려준다.
 *
 * 토글이 아니라 원하는 상태를 지정한다. 같은 요청을 두 번 보내도 결과가 같아서
 * 따닥 눌러 요청이 두 번 나가도 카운트가 어긋나지 않는다.
 */
export async function setPostLike(postId, liked) {
  const { data } = liked
    ? await client.put(`/api/posts/${postId}/like`)
    : await client.delete(`/api/posts/${postId}/like`)
  return data
}

/** 댓글 작성. 생성된 댓글을 그대로 돌려준다. */
export async function addComment(postId, content) {
  const { data } = await client.post(`/api/posts/${postId}/comments`, { content })
  return data
}

/**
 * 글 수정. 제목과 본문만 바뀐다.
 *
 * 어떤 기록에 대한 글인지(meal_history_id)는 못 바꾼다.
 * 그게 바뀌면 본문과 레시피가 어긋나는데, 그건 수정이 아니라 다른 글이다.
 */
export async function updatePost(postId, { title, content }) {
  const { data } = await client.put(`/api/posts/${postId}`, { title, content })
  return data
}

/** 글 삭제. 내 글만 지울 수 있다. 성공하면 204라 본문이 없다. */
export async function removePost(postId) {
  await client.delete(`/api/posts/${postId}`)
}

/**
 * 조회수 올리기.
 *
 * 상세를 GET 할 때 서버가 같이 올리지 않는다. GET 은 몇 번을 보내도 결과가
 * 같아야 하기 때문이다. 화면이 상세를 열 때 이 함수를 한 번 부른다.
 */
export async function countPostView(postId) {
  const { data } = await client.post(`/api/posts/${postId}/view`)
  return data
}

/** 댓글 수정. 내 댓글만 고칠 수 있다. */
export async function updateComment(postId, commentId, content) {
  const { data } = await client.put(
    `/api/posts/${postId}/comments/${commentId}`,
    { content },
  )
  return data
}

/** 댓글 삭제. 내 댓글만 지울 수 있다. */
export async function removeComment(postId, commentId) {
  await client.delete(`/api/posts/${postId}/comments/${commentId}`)
}

/**
 * 인기 레시피 — 고른 기간에 **새로 받은** 좋아요가 많은 순.
 *
 * 누적 좋아요가 아니다. 누적으로 세면 오래된 글이 영원히 1위라서
 * 순위가 움직이지 않는다.
 *
 * start · end 는 'YYYY-MM-DD' 한국 날짜다. 서버가 UTC 로 바꿔서 비교한다.
 * 안 주면 오늘 포함 최근 30일이다.
 */
export async function getPopular({ start, end, limit } = {}) {
  const { data } = await client.get('/api/posts/popular', {
    params: { start, end, limit },
  })
  return data
}
