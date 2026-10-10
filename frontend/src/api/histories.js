import client from './client'
import { authHeader } from '../utils/token'

// 내 식사 기록 API. 로그인이 필요하다.
//
// "이걸로 먹을래요" 를 누른 레시피가 여기 쌓인다. 그 기록이 세 곳에서 쓰인다
//   마이페이지의 요약 카드 (건수 · 누적 재료비 · 연속 기록일)
//   내 기록 목록 화면
//   커뮤니티 후기의 근거 — 먹어본 사람만 후기를 쓸 수 있게 하려는 것이다
//
// 수정이 없다. 기록은 "그때 이걸 먹었다" 는 사실이라 고칠 대상이 아니다.

// 내 기록 한 페이지. { items, total, page, size, total_pages } 가 온다.
// shared: false 를 넘기면 아직 커뮤니티에 공유 안 한 기록만 온다.
//
// shared 를 아예 안 넘기면 전부 온다. 후기 쓰기 화면에서 false 로
// 부른다 - 이미 후기를 쓴 기록은 또 고를 수 없어서 목록에서 뺀다.
//
// params 로 넘기면 axios 가 ?page=1&size=20 모양으로 붙여준다.
// 값이 undefined 인 칸은 알아서 빠지므로, 안 넘긴 조건은 주소에 안 나간다
export const getHistories = async (options) => {
  const response = await client.get('/api/histories', {
    params: {
      page: options.page || 1,
      size: options.size,
      shared: options.shared,
    },
    headers: authHeader(),
  })
  return response.data
}

// 건수 · 누적 재료비 · 연속 방문일
//
// 목록과 주소가 나뉘어 있다. 마이페이지는 요약만 쓰고 목록은 안 쓰는데,
// 같이 주면 카드 세 개를 그리려고 기록 20건을 같이 받아간다.
//
// 화면에서 셀 수 없는 값이다 — 목록은 한 페이지(20건)뿐이라
// 전체 건수나 연속 기록일을 알 방법이 없다
export const getHistoryStats = async () => {
  const response = await client.get('/api/histories/stats', { headers: authHeader() })
  return response.data
}

// '이걸로 먹을래요' 를 눌렀을 때 기록으로 남긴다
//
// 레시피 화면이 받은 값(getRecipe 의 응답)을 그대로 넘긴다. 그래서
// 칸 이름이 recipe_json 처럼 밑줄 모양이다 - 다른 함수처럼 바꿔 적지
// 않는 이유는, 바꿔 적으면 화면이 받은 것을 한 번 더 옮겨 담아야 한다.
//
// 레시피를 통째로 저장하는 이유 — 나중에 레시피 파일이 바뀌어도
// 내가 그때 만든 것은 그대로 남아야 한다
export const createHistory = async (recipe) => {
  const response = await client.post(
    '/api/histories',
    {
      food_name: recipe.food_name,
      recipe_name: recipe.recipe_name,
      recipe_json: recipe.recipe_json,
      ingredients_json: recipe.ingredients_json,
      substitutes_json: recipe.substitutes_json,
      estimated_cost: recipe.estimated_cost,
    },
    { headers: authHeader() },
  )
  return response.data
}

// 기록 삭제. 커뮤니티에 공유한 기록은 지울 수 없다 (409 에러).
//
// 후기가 이 기록을 "실제로 먹어봤다" 는 근거로 가리키고 있다.
// 후기를 먼저 지우면 이 기록도 지울 수 있다.
export const removeHistory = async (id) => {
  await client.delete(`/api/histories/${id}`, { headers: authHeader() })
}
