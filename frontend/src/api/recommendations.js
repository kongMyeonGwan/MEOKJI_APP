import client from './client'

// AI 추천 · 레시피 API. 로그인 없이도 부를 수 있다.
// 로그인했으면 서버가 알아서 못 먹는 재료를 빼고 추천한다.
//
// 여기 결과는 DB 에 저장되지 않는다.
// '이걸로 먹을래요' 를 눌러야 createHistory() 로 기록에 남는다.

// AI 음식 추천.
// GET 이 아니라 POST 인 이유는 배열로 보내는 조건이 있고, 부를 때마다
// 새로 만들어야 해서 캐시되면 안 되기 때문이다.
//
// 응답: { condition, items, weather, bargains }
export async function getRecommendations(options) {
  // 안 넘긴 값은 기본값을 쓴다
  let source = '카테고리'
  let limit = 5
  let category = null
  let budgetMin = null
  let budgetMax = null
  let query = null
  let exclude = []
  let useWeather = false
  let useBargains = false
  let lat = null
  let lon = null

  if (options) {
    if (options.source) {
      source = options.source
    }
    if (options.limit) {
      limit = options.limit
    }
    if (options.category) {
      category = options.category
    }
    if (options.budgetMin) {
      budgetMin = options.budgetMin
    }
    if (options.budgetMax) {
      budgetMax = options.budgetMax
    }
    if (options.query) {
      query = options.query
    }
    if (options.exclude) {
      exclude = options.exclude
    }
    if (options.useWeather) {
      useWeather = options.useWeather
    }
    if (options.useBargains) {
      useBargains = options.useBargains
    }
    if (options.lat) {
      lat = options.lat
    }
    if (options.lon) {
      lon = options.lon
    }
  }

  const response = await client.post('/api/recommendations', {
    source: source,
    limit: limit,
    category: category,
    budget_min: budgetMin,
    budget_max: budgetMax,
    query: query,
    exclude: exclude,
    use_weather: useWeather,
    use_bargains: useBargains,
    lat: lat,
    lon: lon,
  })
  return response.data
}

// 레시피 만들기 + 재료 가격.
// 가격을 따로 안 부르는 이유는 화면이 둘을 같이 쓰기 때문이다.
// 나눠두면 레시피가 먼저 뜨고 가격표가 나중에 떠서 화면이 한 번 튄다.
// AI 가 만드는 거라 몇 초 걸릴 수 있다.
export async function getRecipe(foodName) {
  const response = await client.get('/api/recipes/' + encodeURIComponent(foodName))
  return response.data
}
