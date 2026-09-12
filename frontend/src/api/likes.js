import client from './client'

/**
 * 찜한 레시피 API.
 *
 * 전부 로그인이 필요하다. 토큰은 client.js 의 요청 인터셉터가 붙인다.
 *
 * 전에는 브라우저(localStorage)에만 있었다. 그래서 기기를 바꾸면 안 따라오고,
 * 한 브라우저를 여러 사람이 쓰면 앞사람 찜이 보였다 — 저장 키에 회원 구분이
 * 없었기 때문이다. 이제 서버가 회원별로 들고 있다.
 *
 * 비로그인 상태에서 하트를 누르면 화면이 로그인으로 보낸다(FoodCard 등).
 * 그래서 '회원 없는 찜' 이라는 상태 자체가 없다.
 */

/**
 * 내 찜 목록. 최근 찜한 순으로 온다.
 *
 *   { names: ['제육볶음', '김치찌개'], count: 2 }
 *
 * 페이징이 없다. 화면이 "이 음식이 찜돼 있나" 를 판단하려면 목록 전체를
 * 들고 있어야 하기 때문이다 — 한 페이지만 받으면 2페이지에 있는 음식의
 * 하트가 빈 채로 그려진다.
 */
export async function getLikes() {
  const { data } = await client.get('/api/likes')
  return data
}

/**
 * 하트를 누른다. 찜돼 있으면 해제되고, 아니면 찜된다.
 *
 *   { food_name: '김치찌개', liked: true, count: 3 }
 *
 * 켜기와 끄기를 한 함수로 둔 이유는 화면의 동작이 하트 하나를 누르는 것뿐이라
 * 서다. 나누면 화면이 "지금 켜져 있나" 를 먼저 알아야 부를 함수를 정할 수 있고,
 * 그 판단이 서버와 어긋나면 하트가 반대로 움직인다.
 *
 * count 를 같이 받는다. 상단바 뱃지가 쓰는 값이라 화면이 직접 세지 않는다.
 */
export async function toggleLike(foodName) {
  const { data } = await client.put('/api/likes', { food_name: foodName })
  return data
}

/** 찜 전체 비우기. 서버는 행을 지우지 않고 use_yn='N' 으로 바꾼다. */
export async function clearLikes() {
  const { data } = await client.delete('/api/likes')
  return data
}
