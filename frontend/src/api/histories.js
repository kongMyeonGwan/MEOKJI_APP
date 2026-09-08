import client from './client'

/**
 * 내 식사 기록 API (STEP 15~16).
 * 전부 로그인이 필요하다. 토큰은 client.js의 요청 인터셉터가 붙인다.
 */

/**
 * 내 기록 전체. 최신순으로 온다.
 *
 * 각 항목의 shared_post_id 는 DB 컬럼이 아니라 서버가 계산해 붙인 값이다.
 * 값이 있으면 이미 커뮤니티로 공유한 기록이라 다시 공유할 수 없다
 * (uq_reviews_meal 함수 기반 UNIQUE 인덱스).
 */
export async function getHistories() {
  const { data } = await client.get('/api/histories')
  return data
}

/** 건수 · 누적 재료비 · 연속 방문일. */
export async function getHistoryStats() {
  const { data } = await client.get('/api/histories/stats')
  return data
}

/**
 * '이걸로 먹을래요' — 확정한 레시피를 기록으로 남긴다.
 *
 * getRecipe() 가 돌려준 값을 그대로 넘기면 된다.
 * 이 시점의 값이 스냅샷으로 저장되고, 나중에 AI 가 같은 음식으로 다른
 * 레시피를 만들어도 저장된 기록은 바뀌지 않는다.
 */
export async function createHistory(recipe) {
  const { data } = await client.post('/api/histories', {
    food_name: recipe.food_name,
    recipe_name: recipe.recipe_name,
    recipe_json: recipe.recipe_json,
    ingredients_json: recipe.ingredients_json,
    substitutes_json: recipe.substitutes_json,
    estimated_cost: recipe.estimated_cost,
  })
  return data
}

/**
 * 기록 삭제. 성공하면 204라 본문이 없다.
 * 커뮤니티에 공유한 기록은 409 가 난다 (글을 먼저 지워야 한다).
 */
export async function removeHistory(id) {
  await client.delete(`/api/histories/${id}`)
}
