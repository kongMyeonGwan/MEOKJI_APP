import client from './client'
import { authHeader } from '../utils/token'

// AI 추천 · 레시피 API. 로그인 없이도 부를 수 있다.
// 로그인했으면 서버가 못 먹는 재료를 빼고 추천한다.

// AI 음식 추천. { condition, items, weather, bargains, page, total, totalPages } 가 온다.
// 안 넘긴 값은 기본값을 쓴다. page 를 안 넘기면 1페이지(앞에서 limit 개).
export const getRecommendations = async (options) => {
  const response = await client.post('/api/recommendations', {
    source: options.source || '카테고리',
    limit: options.limit || 5,
    page: options.page || 1,
    category: options.category || null,
    budgetMin: options.budgetMin || null,
    budgetMax: options.budgetMax || null,
    query: options.query || null,
    exclude: options.exclude || [],
    useWeather: options.useWeather || false,
    useBargains: options.useBargains || false,
    lat: options.lat || null,
    lon: options.lon || null,
  }, { headers: authHeader() })
  return response.data
}

// 레시피 + 재료 가격. AI 가 만들어서 몇 초 걸릴 수 있다.
export const getRecipe = async (foodName) => {
  const response = await client.get(`/api/recipes/${encodeURIComponent(foodName)}`, {
    headers: authHeader(),
  })
  return response.data
}
