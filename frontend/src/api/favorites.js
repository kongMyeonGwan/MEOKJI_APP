import client from './client'
import { authHeader } from '../utils/token'

// 찜한 레시피 API. 로그인이 필요하다.
// 찜은 favorite 으로 부른다. 후기 글 '좋아요(like)' 와는 다른 기능이다 (api/reviews.js 의 setReviewLike).

// 내 찜 목록. { names: ['제육볶음', '김치찌개'], items: [...], count: 2 }
export const getFavorites = async () => {
  const response = await client.get('/api/favorites', { headers: authHeader() })
  return response.data
}

// 하트 누르기. 찜돼 있으면 풀리고 아니면 찜된다.
// { foodName: '김치찌개', favorited: true, count: 3 } 가 온다.
export const toggleFavorite = async (foodName) => {
  const response = await client.put('/api/favorites', { foodName: foodName }, { headers: authHeader() })
  return response.data
}

// 찜 전체 비우기
export const clearFavorites = async () => {
  await client.delete('/api/favorites', { headers: authHeader() })
}
