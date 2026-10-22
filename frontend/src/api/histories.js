import client from './client'
import { authHeader } from '../utils/token'

// 내 식사 기록 API. 로그인이 필요하다.

// 내 기록 한 페이지. { items, total, page, size, totalPages } 가 온다.
// shared: false 를 넘기면 아직 커뮤니티에 공유 안 한 기록만 온다.
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
export const getHistoryStats = async () => {
  const response = await client.get('/api/histories/stats', { headers: authHeader() })
  return response.data
}

// '이걸로 먹을래요' 를 눌렀을 때 기록으로 남긴다
export const createHistory = async (recipe) => {
  const response = await client.post(
    '/api/histories',
    {
      foodName: recipe.foodName,
      recipeName: recipe.recipeName,
      recipeJson: recipe.recipeJson,
      ingredientsJson: recipe.ingredientsJson,
      substitutesJson: recipe.substitutesJson,
      estimatedCost: recipe.estimatedCost,
    },
    { headers: authHeader() },
  )
  return response.data
}

// 기록 삭제. 커뮤니티에 공유한 기록은 지울 수 없다 (409 에러).
export const removeHistory = async (id) => {
  await client.delete(`/api/histories/${id}`, { headers: authHeader() })
}
