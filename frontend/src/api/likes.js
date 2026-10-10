import client from './client'
import { authHeader } from '../utils/token'

// 찜한 레시피 API. 로그인이 필요하다.
//
// 원래 브라우저(localStorage)에 두었다. 기기를 바꾸면 안 따라오고,
// 한 브라우저를 여러 사람이 쓰면 앞사람 찜이 보여서 서버로 옮겼다.
//
// 개별 삭제가 없다. 하트를 다시 누르면 풀리므로 toggle 하나로 끝난다 -
// clearLikes 는 설정 화면의 "찜 비우기" 전용이다.

// 내 찜 목록. { names: ['제육볶음', '김치찌개'], count: 2 }
//
// items 도 같이 온다 (음식 카드를 그릴 분류 · 조리시간 · 난이도).
// names 와 items 가 같은 것을 두 번 담는 것처럼 보이지만 쓰는 곳이 다르다 -
// names 는 추천 화면이 "이 음식을 찜했나" 를 볼 때, items 는 찜 목록
// 화면이 카드를 그릴 때 쓴다.
export const getLikes = async () => {
  const response = await client.get('/api/likes', { headers: authHeader() })
  return response.data
}

// 하트 누르기. 찜돼 있으면 풀리고 아니면 찜된다.
// { food_name: '김치찌개', liked: true } 가 온다.
//
// 화면이 "켜라/꺼라" 를 보내지 않는다. 서버가 지금 상태를 보고 뒤집는다 -
// 화면이 정하면, 다른 기기에서 이미 푼 찜을 또 풀려고 보낼 수 있다.
//
// count(내 찜 전체 개수)도 같이 온다. 화면이 1 을 더하거나 빼서
// 스스로 맞추면 다른 기기에서 누른 것과 어긋난다.
//
// POST 가 아니라 PUT 인 이유 — "새로 만든다" 가 아니라 "이 상태로
// 맞춰라" 에 가깝다
export const toggleLike = async (foodName) => {
  const response = await client.put('/api/likes', { food_name: foodName }, { headers: authHeader() })
  return response.data
}

// 찜 전체 비우기
export const clearLikes = async () => {
  await client.delete('/api/likes', { headers: authHeader() })
}
