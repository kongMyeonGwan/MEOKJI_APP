import client from './client'

/**
 * AI 추천 · 레시피 · 재료 가격 API (STEP 10~19).
 *
 * 셋 다 로그인 없이 부를 수 있다. 로그인했으면 client.js 가 토큰을 붙여주고,
 * 서버가 알아서 못 먹는 재료(Ban)를 프롬프트에 넣는다.
 *
 * 이 API 들의 결과는 **DB 에 저장되지 않는다.**
 * 사용자가 '이걸로 먹을래요'를 눌렀을 때만 createHistory() 로 기록에 남는다.
 */

/**
 * AI 음식 추천.
 *
 * GET 이 아니라 POST 인 이유는 두 가지다.
 *   - 냉장고 재료처럼 배열로 보내는 조건이 있어 주소창에 담기 어렵다
 *   - 부를 때마다 새로 만들어내므로 캐시되면 안 된다
 *
 * 응답: { condition, items, weather }
 *   condition  "서울 · 맑음 28° · 못 먹는 재료 3건 제외" — AI 추천 기록에 쓴다
 *   items      화면에 그릴 음식 카드 목록
 *   weather    useWeather 를 켰을 때만 채워진다
 */
export async function getRecommendations({
  source = '카테고리',
  limit = 5,
  category = null,
  budgetMin = null,
  budgetMax = null,
  query = null,
  ingredients = [],
  exclude = [],
  useWeather = false,
  lat = null,
  lon = null,
} = {}) {
  const { data } = await client.post('/api/recommendations', {
    source,
    limit,
    category,
    budget_min: budgetMin,
    budget_max: budgetMax,
    query,
    ingredients,
    exclude,
    use_weather: useWeather,
    lat,
    lon,
  })
  return data
}

/**
 * 레시피 생성 + 재료 가격.
 *
 * 가격을 따로 부르지 않는 이유는 화면이 둘을 같이 쓰기 때문이다.
 * 나눠 두면 레시피가 먼저 뜨고 가격표가 나중에 나타나 화면이 한 번 튄다.
 *
 * AI 가 만들어내는 값이라 응답이 몇 초 걸릴 수 있다
 * (client.js 의 timeout 이 30초인 것도 이 때문이다).
 */
export async function getRecipe(foodName) {
  const { data } = await client.get(
    `/api/recipes/${encodeURIComponent(foodName)}`,
  )
  return data
}

/** 재료 이름 목록으로 판매처별 가격을 조회한다. */
export async function getPrices(ingredients) {
  const { data } = await client.post('/api/prices', { ingredients })
  return data
}
