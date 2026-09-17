import client from './client'

// 찜한 레시피 API. 전부 로그인이 필요하다.
// 토큰은 client.js 가 알아서 붙여준다.

// 내 찜 목록. { names: ['제육볶음', '김치찌개'], count: 2 }
// 페이징이 없다. 화면이 하트를 칠하려면 목록 전체가 있어야 하기 때문이다.
export async function getLikes() {
  const response = await client.get('/api/likes')
  return response.data
}

// 하트 누르기. 찜돼 있으면 풀리고 아니면 찜된다.
// { food_name: '김치찌개', liked: true, count: 3 }
export async function toggleLike(foodName) {
  const response = await client.put('/api/likes', { food_name: foodName })
  return response.data
}

// 찜 전체 비우기
export async function clearLikes() {
  const response = await client.delete('/api/likes')
  return response.data
}
