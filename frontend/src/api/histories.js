import client from './client'

/**
 * 내 식사 기록 API (STEP 16).
 * 로그인이 필요하다. 토큰은 client.js의 요청 인터셉터가 붙인다.
 */

/**
 * 내 기록 전체. 최신순으로 온다.
 *
 * 각 항목의 shared_post_id 는 DB 컬럼이 아니라 서버가 계산해 붙인 값이다.
 * 값이 있으면 이미 커뮤니티로 공유한 기록이라 다시 공유할 수 없다
 * (posts.meal_history_id 의 UNIQUE 제약).
 */
export async function getHistories() {
  const { data } = await client.get('/api/histories')
  return data
}
