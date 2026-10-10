import client from './client'
import { authHeader } from '../utils/token'

// AI 추천 · 레시피 API. 로그인 없이도 부를 수 있다.
// 로그인했으면 서버가 못 먹는 재료를 빼고 추천한다.
//
// authHeader() 를 넘기면서 로그인 없이도 되는 이유 — 토큰이 없으면
// authHeader() 가 빈 객체를 돌려준다(utils/token). 서버도 토큰이
// 없을 때를 정상으로 보고, 못 먹는 재료만 안 빼고 추천한다.
//
// 두 함수가 한 파일에 있는 이유 — 화면 흐름이 이어져 있다.
// 추천을 받아 음식을 고르면 그 이름으로 레시피를 부른다.

// AI 음식 추천. { condition, items, weather, bargains } 가 온다.
// 안 넘긴 값은 기본값을 쓴다.
//
// GET 이 아니라 POST 인 이유 — 조건이 열 가지가 넘고 그 중에 목록
// (exclude - 이미 본 메뉴)이 있다. 주소줄에 담으면 끝없이 길어지고,
// GET 이면 브라우저가 캐시해서 새로고침해도 같은 목록이 나온다.
//
// condition 은 "무슨 조건으로 골랐는지" 를 적은 한 줄이다
// ("서울 · 맑음 28° · 한식"). 화면이 목록 위에 그대로 띄운다.
export const getRecommendations = async (options) => {
  const response = await client.post('/api/recommendations', {
    // || 로 기본값을 넣는다. 안 넘긴 칸이 undefined 로 가면
    // 서버에서 그 칸이 아예 없는 것이 되는데, null 로 보내면
    // "값이 없다" 가 분명해진다
    //
    // 조건 칸은 null, 참/거짓 칸은 false, 목록 칸은 [] 로 맞춘다
    source: options.source || '카테고리',
    limit: options.limit || 5,
    category: options.category || null,
    budget_min: options.budgetMin || null,
    budget_max: options.budgetMax || null,
    query: options.query || null,
    // 이미 본 메뉴. "다른 메뉴 추천" 을 누를 때 화면이 모아 보낸다 -
    // 서버가 기억하지 않는 이유는 로그인 안 한 사람을 가릴 수 없어서다
    exclude: options.exclude || [],
    // 켜면 날씨를 보고 점수를 매긴다. 기본으로 안 켜는 이유 -
    // 바깥 API 를 불러야 해서 그만큼 느리다
    use_weather: options.useWeather || false,
    // 켜면 "요즘 싼 재료" 도 같이 온다. 대시보드의 그 카드만 켠다
    use_bargains: options.useBargains || false,
    // 브라우저가 알려준 내 위치. 저장된 주소보다 이것을 먼저 쓴다
    lat: options.lat || null,
    lon: options.lon || null,
  }, { headers: authHeader() })
  return response.data
}

// 레시피 + 재료 가격. 재료 시세를 하나씩 받아와서 몇 초 걸릴 수 있다.
//
// 재료가 아홉 개면 6초쯤 걸린다. 그동안 화면에 "레시피를 만들고 있어요" 를
// 띄운다. client 의 시간 제한을 30초로 길게 잡아둔 것이 이 때문이다.
//
// 준비된 레시피가 없는 음식이면 404 가 온다. 사용자가 직접 검색한
// 이름으로도 부를 수 있어서, 화면이 그때 안내 문장을 띄운다.
export const getRecipe = async (foodName) => {
  // encodeURIComponent 를 꼭 거친다. 음식 이름이 한글이고 공백도
  // 들어 있어서, 그대로 주소에 넣으면 요청이 깨진다
  const response = await client.get(`/api/recipes/${encodeURIComponent(foodName)}`, {
    headers: authHeader(),
  })
  return response.data
}
