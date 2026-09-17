import client from './client'

// 내 식사 기록 API. 전부 로그인이 필요하다.

// 내 기록 한 페이지. 최신순으로 온다.
// 배열이 아니라 { items, total, page, size, total_pages } 가 온다.
//
// 각 항목의 shared_post_id 에 값이 있으면 이미 커뮤니티에 공유한 기록이라
// 다시 공유할 수 없다.
//
// shared 를 넘기면 서버가 걸러준다.
//   false - 아직 공유 안 한 것만 (글쓰기 화면)
//   true  - 이미 공유한 것만
//   안 넘기면 전부 (기록 화면)
export async function getHistories(options) {
  let page = 1
  let size = undefined
  let shared = undefined

  if (options) {
    if (options.page) {
      page = options.page
    }
    size = options.size
    shared = options.shared
  }

  const response = await client.get('/api/histories', {
    // undefined 는 axios 가 빼고 보낸다. 그러면 서버 기본값이 쓰인다.
    params: { page: page, size: size, shared: shared },
  })
  return response.data
}

// 건수 · 누적 재료비 · 연속 방문일
export async function getHistoryStats() {
  const response = await client.get('/api/histories/stats')
  return response.data
}

// '이걸로 먹을래요' 를 눌렀을 때 기록으로 남긴다.
// getRecipe() 가 준 값을 그대로 넘기면 된다.
// 이때 값이 그대로 저장돼서 나중에 AI 가 다른 레시피를 만들어도 안 바뀐다.
export async function createHistory(recipe) {
  const response = await client.post('/api/histories', {
    food_name: recipe.food_name,
    recipe_name: recipe.recipe_name,
    recipe_json: recipe.recipe_json,
    ingredients_json: recipe.ingredients_json,
    substitutes_json: recipe.substitutes_json,
    estimated_cost: recipe.estimated_cost,
  })
  return response.data
}

// 기록 삭제. 커뮤니티에 공유한 기록은 409 가 난다 (글을 먼저 지워야 한다).
export async function removeHistory(id) {
  await client.delete('/api/histories/' + id)
}
