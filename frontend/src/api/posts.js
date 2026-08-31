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
