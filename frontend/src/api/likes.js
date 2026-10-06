import client from './client'

// 찜한 레시피 API. 로그인이 필요하다.

// 내 찜 목록. { names: ['제육볶음', '김치찌개'], count: 2 }
export const getLikes = async () => {
  const response = await client.get('/api/likes')
  return response.data
}

// 하트 누르기. 찜돼 있으면 풀리고 아니면 찜된다.
// { food_name: '김치찌개', liked: true } 가 온다.
export const toggleLike = async (foodName) => {
  const response = await client.put('/api/likes', { food_name: foodName })
  return response.data
}

// 찜 전체 비우기
export const clearLikes = async () => {
  await client.delete('/api/likes')
}
