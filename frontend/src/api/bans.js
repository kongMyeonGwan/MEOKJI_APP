import client from './client'

/**
 * Ban 재료 API (STEP 9).
 * 세 함수 모두 로그인이 필요하다. 토큰은 client.js의 요청 인터셉터가 붙인다.
 */

/** 내 Ban 전체. 등록 순으로 온다. */
export async function getBans() {
  const { data } = await client.get('/api/bans')
  return data
}

/**
 * Ban 추가. 생성된 행을 그대로 돌려준다(서버가 채운 id/created_at 포함).
 * 이미 등록한 재료면 409가 난다.
 */
export async function addBan({ ingredientName, banType }) {
  const { data } = await client.post('/api/bans', {
    ingredient_name: ingredientName,
    ban_type: banType,
  })
  return data
}

/** Ban 삭제. 성공하면 204라 본문이 없다. */
export async function removeBan(id) {
  await client.delete(`/api/bans/${id}`)
}
