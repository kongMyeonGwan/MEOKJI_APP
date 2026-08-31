/**
 * 화면 개발용 하드코딩 데이터.
 *
 * 백엔드 API가 준비되기 전에 화면을 먼저 완성하기 위한 임시 데이터다.
 * 필드 이름과 구조를 DB 스키마(db/01_schema.sql) 및 API 응답과 똑같이 맞춰뒀다.
 * 나중에 각 페이지에서 이 import를 실제 api 호출로 바꾸기만 하면 된다.
 *
 *   import { MOCK_HISTORIES } from '../mocks'      ->   const data = await getHistories()
 *
 * art 는 사진이 없는 동안 쓰는 SVG 일러스트의 종류다
 * (stew · soup · noodle · stirfry · pancake · donburi).
 * 실제 이미지가 생기면 image_url 을 채우면 FoodThumb 이 그것을 우선 사용한다.
 *
 * 실제 연동이 끝나면 이 파일과 mocks/ 폴더를 통째로 지운다.
 */

// ---------------------------------------------------------
// 날씨 (STEP 12)
// ---------------------------------------------------------
// condition 은 화면 배경색 분기에 쓰인다 (CLEAR · CLOUDS · RAIN · SNOW).
// condition_text 는 사람이 읽는 문구라 표시용으로만 쓰고 분기 조건으로 삼지 않는다.
// 번역이나 표현이 바뀌면 조건문이 조용히 깨지기 때문이다.
//
// comment 는 왼쪽 날씨 칸에서 "오늘은 이런 날"을 말하는 문장이고,
// pick_lead 는 오른쪽 추천 칸에서 "그래서 이걸 드세요"로 넘어가는 문장이다.
// 히어로가 왼쪽(근거) -> 오른쪽(답)으로 읽히므로 문장도 둘로 나눠 둔다.
// STEP 12에서 서버가 이 형태로 내려준다.
export const MOCK_WEATHER = {
  city: '서울특별시',
  temp: 28,
  feels_like: 30,
  humidity: 52,
  condition: 'CLEAR',
  condition_text: '맑음',
  comment: '오늘은 맑고 더운 날씨에요!\n시원하고 담백한 메뉴가 잘 어울려요',
  pick_lead: '더운 날씨엔 시원한 한 그릇 어떠세요?',
}

// ---------------------------------------------------------
// AI 음식 추천 (STEP 13) — DB에 저장하지 않는 후보 목록
//
// estimated_cost 를 넓게 흩어놨다. 예산 필터가 실제로 걸러지는지
// 확인하려면 값이 다양해야 한다. 2,600원부터 24,000원까지 있고,
// 마지막 한 건은 일부러 BUDGET_MAX(20,000)를 넘겨뒀다.
// 슬라이더를 끝까지 밀었을 때 그 건이 사라지면 "상한 없음" 처리가 깨진 것이다.
//
// category 는 MOCK_CATEGORIES 의 label 과 같은 문자열을 쓴다.
// 룰렛·카테고리 칩·/recommend?category= 가 전부 이 값으로 비교한다.
// MOCK_CATEGORIES 의 분류마다 최소 2건씩 있어야 어느 걸 골라도 빈 화면이 안 뜬다.
// ---------------------------------------------------------
export const MOCK_RECOMMENDATIONS = [
  {
    food_name: '김치찌개',
    art: 'stew',
    image_url: null,
    category: '한식',
    reason: '따뜻한 국물로 속을 든든하게 채워주는 한국인의 찌개',
    cook_time_minutes: 25,
    difficulty: '쉬움',
    estimated_cost: 7800,
    tags: ['국물', '얼큰', '한식'],
  },
  {
    food_name: '들깨 수제비',
    art: 'soup',
    image_url: null,
    category: '한식',
    reason: '고소한 들깨와 쫄깃한 수제비의 부드럽고 든든한 한 끼',
    cook_time_minutes: 30,
    difficulty: '쉬움',
    estimated_cost: 6200,
    tags: ['국물', '한식'],
  },
  {
    food_name: '닭갈비',
    art: 'stirfry',
    image_url: null,
    category: '한식',
    reason: '매콤한 양념과 닭고기의 조화로 밥도둑이 따로 없어요',
    cook_time_minutes: 30,
    difficulty: '보통',
    estimated_cost: 8000,
    tags: ['매콤', '볶음', '한식'],
  },
  {
    food_name: '간단 돈까스',
    art: 'donburi',
    image_url: null,
    category: '일식',
    reason: '바삭한 튀김옷과 촉촉한 고기의 완벽한 조화',
    cook_time_minutes: 20,
    difficulty: '보통',
    estimated_cost: 6900,
    tags: ['튀김', '간편', '일식'],
  },
  {
    food_name: '된장찌개',
    art: 'stew',
    image_url: null,
    category: '국탕',
    reason: '구수한 된장 향으로 한국인의 소울푸드',
    cook_time_minutes: 20,
    difficulty: '쉬움',
    estimated_cost: 5200,
    tags: ['국물', '건강', '한식'],
  },
  {
    food_name: '콩나물국밥',
    art: 'noodle',
    image_url: null,
    category: '국탕',
    reason: '가장 저렴하게 따뜻한 국물을 먹을 수 있어요. 재료가 3가지뿐',
    cook_time_minutes: 20,
    difficulty: '쉬움',
    estimated_cost: 4200,
    tags: ['국물', '해장'],
  },
  {
    food_name: '순두부찌개',
    art: 'stew',
    image_url: null,
    category: '한식',
    reason: '부드럽고 자극이 적어 늦은 저녁에도 부담이 없어요',
    cook_time_minutes: 25,
    difficulty: '쉬움',
    estimated_cost: 9800,
    tags: ['국물', '한식'],
  },
  {
    food_name: '소고기 미역국',
    art: 'soup',
    image_url: null,
    category: '국탕',
    reason: '재료비는 높지만 한 번 끓이면 여러 끼를 먹을 수 있어요',
    cook_time_minutes: 50,
    difficulty: '보통',
    estimated_cost: 15400,
    tags: ['국물', '보양'],
  },

  // --- 중식 ---
  {
    food_name: '마파두부',
    art: 'stirfry',
    image_url: null,
    category: '중식',
    reason: '두부 한 모로 얼큰한 한 끼. 밥에 얹으면 덮밥이 돼요',
    cook_time_minutes: 20,
    difficulty: '쉬움',
    estimated_cost: 6500,
    tags: ['매콤', '볶음', '중식'],
  },
  {
    food_name: '간짜장',
    art: 'noodle',
    image_url: null,
    category: '중식',
    reason: '춘장만 있으면 배달보다 싸게, 더 되직하게 만들 수 있어요',
    cook_time_minutes: 30,
    difficulty: '보통',
    estimated_cost: 5800,
    tags: ['면', '중식'],
  },

  // --- 일식 ---
  {
    food_name: '연어 사케동',
    art: 'donburi',
    image_url: null,
    category: '일식',
    reason: '불을 쓰지 않아요. 회 한 팩과 밥이면 완성됩니다',
    cook_time_minutes: 10,
    difficulty: '쉬움',
    estimated_cost: 13500,
    tags: ['덮밥', '노불', '일식'],
  },

  // --- 양식 ---
  {
    food_name: '크림 파스타',
    art: 'noodle',
    image_url: null,
    category: '양식',
    reason: '생크림 대신 우유로도 충분히 진하게 나와요',
    cook_time_minutes: 25,
    difficulty: '보통',
    estimated_cost: 8900,
    tags: ['면', '양식'],
  },
  {
    food_name: '함박스테이크',
    art: 'pancake',
    image_url: null,
    category: '양식',
    reason: '다진 고기를 뭉쳐 굽기만 하면 되는데 근사해 보여요',
    cook_time_minutes: 35,
    difficulty: '보통',
    estimated_cost: 11200,
    tags: ['고기', '양식'],
  },
  {
    food_name: '채끝 스테이크',
    art: 'pancake',
    image_url: null,
    category: '양식',
    reason: '재료비가 가장 높아요. 특별한 날에 어울립니다',
    cook_time_minutes: 25,
    difficulty: '어려움',
    estimated_cost: 24000,
    tags: ['고기', '양식', '기념일'],
  },

  // --- 분식 ---
  {
    food_name: '떡볶이',
    art: 'stirfry',
    image_url: null,
    category: '분식',
    reason: '재료가 단순해서 실패할 구석이 거의 없어요',
    cook_time_minutes: 15,
    difficulty: '쉬움',
    estimated_cost: 4800,
    tags: ['매콤', '분식'],
  },
  {
    food_name: '충무김밥',
    art: 'donburi',
    image_url: null,
    category: '분식',
    reason: '속을 넣지 않아 김밥 중에 가장 빠르고 저렴해요',
    cook_time_minutes: 20,
    difficulty: '쉬움',
    estimated_cost: 3500,
    tags: ['간편', '분식'],
  },

  // --- 아시안 ---
  {
    food_name: '팟타이',
    art: 'noodle',
    image_url: null,
    category: '아시안',
    reason: '새콤달콤한 소스가 입맛 없을 때 잘 들어가요',
    cook_time_minutes: 25,
    difficulty: '보통',
    estimated_cost: 9600,
    tags: ['면', '볶음', '아시안'],
  },
  {
    food_name: '소고기 쌀국수',
    art: 'soup',
    image_url: null,
    category: '아시안',
    reason: '맑은 국물이라 속이 편해요. 해장으로도 좋습니다',
    cook_time_minutes: 40,
    difficulty: '보통',
    estimated_cost: 8200,
    tags: ['국물', '면', '아시안'],
  },

  // --- 샐러드 ---
  {
    food_name: '닭가슴살 샐러드',
    art: 'pancake',
    image_url: null,
    category: '샐러드',
    reason: '단백질을 챙기면서 조리는 10분이면 끝나요',
    cook_time_minutes: 10,
    difficulty: '쉬움',
    estimated_cost: 7400,
    tags: ['건강', '다이어트', '샐러드'],
  },
  {
    food_name: '리코타 치즈 샐러드',
    art: 'pancake',
    image_url: null,
    category: '샐러드',
    reason: '치즈를 직접 만들면 밖에서 사 먹는 맛이 나요',
    cook_time_minutes: 45,
    difficulty: '보통',
    estimated_cost: 10800,
    tags: ['치즈', '샐러드'],
  },

  // --- 디저트 ---
  {
    food_name: '바나나 팬케이크',
    art: 'pancake',
    image_url: null,
    category: '디저트',
    reason: '밀가루 없이 바나나와 계란만으로 부칠 수 있어요',
    cook_time_minutes: 15,
    difficulty: '쉬움',
    estimated_cost: 4900,
    tags: ['간식', '디저트'],
  },
  {
    food_name: '딸기 요거트볼',
    art: 'pancake',
    image_url: null,
    category: '디저트',
    reason: '불 없이 담기만 하면 되고, 아침 대용으로도 괜찮아요',
    cook_time_minutes: 5,
    difficulty: '쉬움',
    estimated_cost: 6100,
    tags: ['노불', '간식', '디저트'],
  },

  // --- 간편식 ---
  {
    food_name: '참치마요 덮밥',
    art: 'donburi',
    image_url: null,
    category: '간편식',
    reason: '통조림 하나로 끝나요. 자취 첫 요리로 자주 꼽힙니다',
    cook_time_minutes: 10,
    difficulty: '쉬움',
    estimated_cost: 3800,
    tags: ['덮밥', '간편'],
  },
  {
    food_name: '전자레인지 계란찜',
    art: 'soup',
    image_url: null,
    category: '간편식',
    reason: '가장 저렴한 후보예요. 냄비도 필요 없습니다',
    cook_time_minutes: 8,
    difficulty: '쉬움',
    estimated_cost: 2600,
    tags: ['간편', '노불'],
  },

  // --- 더울 때 (MOCK_WEATHER 가 28° 맑음이라 대시보드 추천이 여기서 나온다) ---
  //
  // 이 다섯 건만 rating / rating_count 를 들고 있다.
  // 별점은 히어로(WeatherPick)에서만 쓰고 카드 목록에는 안 나오는데,
  // 히어로에 올라오는 후보가 지금은 이 다섯 건뿐이기 때문이다.
  // 값이 없는 음식이 히어로에 올라와도 별점 줄만 빠지고 나머지는 그대로 그려진다.
  {
    food_name: '콩국수',
    art: 'noodle',
    image_url: null,
    category: '한식',
    reason: '차가운 콩국물이 더위를 식혀줘요. 불은 면 삶을 때만 씁니다',
    cook_time_minutes: 25,
    difficulty: '보통',
    estimated_cost: 7200,
    rating: 4.8,
    rating_count: 256,
    tags: ['더운날', '시원한음식', '여름메뉴'],
  },
  {
    food_name: '물냉면',
    art: 'noodle',
    image_url: null,
    category: '한식',
    reason: '얼음 동동 띄운 육수 한 사발이면 더위가 가십니다',
    cook_time_minutes: 20,
    difficulty: '쉬움',
    estimated_cost: 8500,
    rating: 4.6,
    rating_count: 412,
    tags: ['더운날', '시원한음식', '면'],
  },
  {
    food_name: '오이냉국',
    art: 'soup',
    image_url: null,
    category: '국탕',
    reason: '새콤한 냉국은 입맛 없는 더운 날에 특히 잘 넘어가요',
    cook_time_minutes: 10,
    difficulty: '쉬움',
    estimated_cost: 3200,
    rating: 4.3,
    rating_count: 88,
    tags: ['시원한음식', '노불', '여름메뉴'],
  },
  {
    food_name: '비빔국수',
    art: 'noodle',
    image_url: null,
    category: '분식',
    reason: '새콤달콤 매콤한 양념이 처진 입맛을 깨워줍니다',
    cook_time_minutes: 15,
    difficulty: '쉬움',
    estimated_cost: 5400,
    rating: 4.7,
    rating_count: 174,
    tags: ['매콤', '면', '여름메뉴'],
  },
  {
    food_name: '자루소바',
    art: 'noodle',
    image_url: null,
    category: '일식',
    reason: '메밀면을 차게 헹궈 츠유에 찍어 먹어요. 속이 편합니다',
    cook_time_minutes: 15,
    difficulty: '쉬움',
    estimated_cost: 7600,
    rating: 4.5,
    rating_count: 131,
    tags: ['시원한음식', '면', '일식'],
  },
]

// ---------------------------------------------------------
// 오늘 날씨에 맞춘 추천 (대시보드 히어로)
//
// **화면에서 날씨↔음식을 매칭하지 않는다.** 실제로는 STEP 13에서 서버가
// 날씨를 프롬프트에 넣어 이미 골라 정렬한 목록을 내려준다. 화면은 받은 순서대로
// 그리기만 한다. 여기서 매칭 로직을 짜두면 API가 붙을 때 통째로 걷어내야 한다.
//
// 그래서 이 배열은 "서버가 이미 골라준 결과"를 흉내낸 것이고,
// 이름으로 찾아 잇는 코드도 mocks 안에만 둔다(이 파일은 연동이 끝나면 통째로 지운다).
// 순서가 곧 추천 순위다. 첫 번째가 "오늘의 한 접시"로 크게 뜬다.
// ---------------------------------------------------------
const WEATHER_PICK_NAMES = ['콩국수', '물냉면', '오이냉국', '비빔국수', '자루소바']

export const MOCK_WEATHER_PICKS = WEATHER_PICK_NAMES.map((name) =>
  MOCK_RECOMMENDATIONS.find((food) => food.food_name === name)
)

// ---------------------------------------------------------
// 카테고리 (대시보드 가로 스크롤 칩)
//
// art 는 칩 안에 그릴 SVG 일러스트 종류다. 사진이 없으므로
// 비슷한 그림을 하나씩 붙여뒀다. '전체'만 그림 대신 격자 아이콘을 쓴다.
// ---------------------------------------------------------
export const MOCK_CATEGORIES = [
  { key: 'ALL', label: '전체', art: null },
  { key: 'KR', label: '한식', art: 'stew' },
  { key: 'CN', label: '중식', art: 'stirfry' },
  { key: 'JP', label: '일식', art: 'donburi' },
  { key: 'WEST', label: '양식', art: 'pancake' },
  { key: 'SNACK', label: '분식', art: 'stirfry' },
  { key: 'ASIA', label: '아시안', art: 'noodle' },
  { key: 'SALAD', label: '샐러드', art: 'pancake' },
  { key: 'SOUP', label: '국탕', art: 'soup' },
  { key: 'DESSERT', label: '디저트', art: 'pancake' },
  { key: 'EASY', label: '간편식', art: 'donburi' },
]

// ---------------------------------------------------------
// 음식 추천 룰렛
//
// 8칸이라 45도씩 나뉜다. 색은 브랜드 주황 계열에서 명도만 바꿔
// 옆칸끼리 붙어 보이지 않게 골랐다.
// ---------------------------------------------------------
export const MOCK_ROULETTE = [
  { label: '한식', color: '#f6c445' },
  { label: '중식', color: '#e8481c' },
  { label: '일식', color: '#f6a23c' },
  { label: '양식', color: '#7fae4e' },
  { label: '분식', color: '#e8703a' },
  { label: '아시안', color: '#f2d16b' },
  { label: '샐러드', color: '#8fbf5f' },
  { label: '국탕', color: '#efb03e' },
]

// ---------------------------------------------------------
// 좋아요 받은 레시피 (대시보드 기간 검색용)
//
// liked_at 을 여러 달에 걸쳐 흩어놨다.
//   기본값(최근 30일)  -> 2026-08 항목만 보인다
//   2025-07-01~07-15   -> 아래 5건이 보인다 (디자인 시안과 동일한 상태)
// ---------------------------------------------------------
export const MOCK_LIKED_RECIPES = [
  {
    id: 10,
    food_name: '김치찌개',
    recipe_name: '돼지고기 김치찌개',
    art: 'stew',
    image_url: null,
    nickname: '집밥러',
    like_count: 34,
    estimated_cost: 7800,
    liked_at: '2026-08-16T10:30:00Z',
  },
  {
    id: 9,
    food_name: '들깨 수제비',
    recipe_name: '고소한 들깨 수제비',
    art: 'soup',
    image_url: null,
    nickname: '요리초보',
    like_count: 21,
    estimated_cost: 6200,
    liked_at: '2026-08-12T05:10:00Z',
  },
  {
    id: 8,
    food_name: '순두부찌개',
    recipe_name: '얼큰 순두부찌개',
    art: 'stew',
    image_url: null,
    nickname: '자취9년차',
    like_count: 18,
    estimated_cost: 9800,
    liked_at: '2026-08-02T12:40:00Z',
  },
  {
    id: 7,
    food_name: '닭갈비',
    recipe_name: '춘천식 닭갈비',
    art: 'stirfry',
    image_url: null,
    nickname: '닭갈비마스터',
    like_count: 47,
    estimated_cost: 8000,
    liked_at: '2026-07-25T08:00:00Z',
  },
  // 기본 기간(최근 30일)에 10건 넘게 걸리도록 채워둔 항목들.
  // 대시보드가 상위 10건에서 자르는지 눈으로 확인하려면 후보가 그보다 많아야 한다.
  {
    id: 20,
    food_name: '제육볶음',
    recipe_name: '매콤 제육볶음',
    art: 'stirfry',
    image_url: null,
    nickname: '고추장러버',
    like_count: 58,
    estimated_cost: 9400,
    liked_at: '2026-08-21T11:20:00Z',
  },
  {
    id: 19,
    food_name: '김치볶음밥',
    recipe_name: '참치 김치볶음밥',
    art: 'donburi',
    image_url: null,
    nickname: '자취9년차',
    like_count: 41,
    estimated_cost: 4300,
    liked_at: '2026-08-19T03:05:00Z',
  },
  {
    id: 18,
    food_name: '잔치국수',
    recipe_name: '멸치육수 잔치국수',
    art: 'noodle',
    image_url: null,
    nickname: '면식가',
    like_count: 29,
    estimated_cost: 3900,
    liked_at: '2026-08-18T07:40:00Z',
  },
  {
    id: 17,
    food_name: '된장찌개',
    recipe_name: '두부 듬뿍 된장찌개',
    art: 'stew',
    image_url: null,
    nickname: '집밥러',
    like_count: 26,
    estimated_cost: 5600,
    liked_at: '2026-08-15T12:00:00Z',
  },
  {
    id: 16,
    food_name: '부대찌개',
    recipe_name: '푸짐한 부대찌개',
    art: 'stew',
    image_url: null,
    nickname: '주말요리사',
    like_count: 24,
    estimated_cost: 12600,
    liked_at: '2026-08-13T09:15:00Z',
  },
  {
    id: 15,
    food_name: '해물파전',
    recipe_name: '바삭 해물파전',
    art: 'pancake',
    image_url: null,
    nickname: '비오는날',
    like_count: 22,
    estimated_cost: 8700,
    liked_at: '2026-08-10T10:50:00Z',
  },
  {
    id: 14,
    food_name: '비빔국수',
    recipe_name: '새콤달콤 비빔국수',
    art: 'noodle',
    image_url: null,
    nickname: '여름입맛',
    like_count: 19,
    estimated_cost: 4100,
    liked_at: '2026-08-08T06:30:00Z',
  },
  {
    id: 13,
    food_name: '카레라이스',
    recipe_name: '야채 듬뿍 카레라이스',
    art: 'donburi',
    image_url: null,
    nickname: '요리초보',
    like_count: 16,
    estimated_cost: 6800,
    liked_at: '2026-08-06T13:25:00Z',
  },
  {
    id: 12,
    food_name: '콩나물국밥',
    recipe_name: '얼큰 콩나물국밥',
    art: 'soup',
    image_url: null,
    nickname: '해장전문',
    like_count: 13,
    estimated_cost: 4600,
    liked_at: '2026-08-04T22:10:00Z',
  },
  {
    id: 11,
    food_name: '떡볶이',
    recipe_name: '국물 떡볶이',
    art: 'stirfry',
    image_url: null,
    nickname: '분식왕',
    like_count: 11,
    estimated_cost: 5200,
    liked_at: '2026-08-01T08:55:00Z',
  },
  // --- 2025년 7월 (시안의 검색 결과와 동일) ---
  {
    id: 5,
    food_name: '김치전',
    recipe_name: '바삭한 김치전 레시피',
    art: 'pancake',
    image_url: null,
    nickname: '비오는날',
    like_count: 64,
    estimated_cost: 4800,
    liked_at: '2025-07-14T09:00:00Z',
  },
  {
    id: 4,
    food_name: '두루치기',
    recipe_name: '돼지고기 두루치기',
    art: 'stirfry',
    image_url: null,
    nickname: '집밥러',
    like_count: 52,
    estimated_cost: 11200,
    liked_at: '2025-07-10T04:30:00Z',
  },
  {
    id: 3,
    food_name: '돈까스',
    recipe_name: '간단 돈까스 레시피',
    art: 'donburi',
    image_url: null,
    nickname: '튀김러버',
    like_count: 47,
    estimated_cost: 9300,
    liked_at: '2025-07-08T11:20:00Z',
  },
  {
    id: 2,
    food_name: '어묵볶음',
    recipe_name: '매콤 어묵볶음',
    art: 'stirfry',
    image_url: null,
    nickname: '반찬왕',
    like_count: 38,
    estimated_cost: 3600,
    liked_at: '2025-07-05T02:10:00Z',
  },
  {
    id: 1,
    food_name: '가지덮밥',
    recipe_name: '가지 덮밥',
    art: 'donburi',
    image_url: null,
    nickname: '여름입맛',
    like_count: 35,
    estimated_cost: 5300,
    liked_at: '2025-07-02T07:45:00Z',
  },
]

// ---------------------------------------------------------
// 확정한 식사 기록 (STEP 15~16) — meal_histories 테이블
//
// shared_post_id : DB 컬럼이 아니라 서버가 계산해 내려주는 값이다.
//   posts.meal_history_id 에 uq_posts_meal(UNIQUE)이 걸려 있어 한 기록은 한 번만
//   공유할 수 있으므로, 이미 공유한 기록은 글쓰기 목록에서 빼고 목록에는 배지를 띄운다.
//   NULL 이면 아직 공유하지 않은 기록.
// ---------------------------------------------------------
export const MOCK_HISTORIES = [
  {
    id: 3,
    shared_post_id: 3,
    food_name: '김치찌개',
    recipe_name: '돼지고기 김치찌개',
    art: 'stew',
    image_url: null,
    estimated_cost: 7800,
    selected_at: '2026-08-16T09:20:00Z',
    recipe_json: {
      cook_time_minutes: 25,
      servings: 2,
      difficulty: '쉬움',
      steps: [
        '돼지고기를 한입 크기로 썰어 팬에 볶는다.',
        '잘 익은 김치를 넣고 3분간 함께 볶는다.',
        '물 500ml를 붓고 센 불에서 끓인다.',
        '두부와 대파를 넣고 5분 더 끓인 뒤 간을 맞춘다.',
      ],
    },
    ingredients_json: [
      { name: '돼지고기 앞다리살', quantity: 200, unit: 'g' },
      { name: '신김치', quantity: 300, unit: 'g' },
      { name: '두부', quantity: 1, unit: '모' },
      { name: '대파', quantity: 1, unit: '대' },
    ],
    substitutes_json: [
      { original: '돼지고기 앞다리살', substitute: '참치캔', reason: '더 간편하게 만들 수 있습니다' },
    ],
  },
  {
    id: 2,
    shared_post_id: null,
    food_name: '들깨 수제비',
    recipe_name: '고소한 들깨 수제비',
    art: 'soup',
    image_url: null,
    estimated_cost: 6200,
    selected_at: '2026-08-15T11:05:00Z',
    recipe_json: {
      cook_time_minutes: 30,
      servings: 2,
      difficulty: '쉬움',
      steps: [
        '밀가루에 물을 조금씩 넣어가며 반죽하고 30분 숙성한다.',
        '멸치 육수를 낸다.',
        '반죽을 얇게 떼어 넣고 끓인다.',
        '들깨가루를 풀고 소금으로 간한다.',
      ],
    },
    ingredients_json: [
      { name: '밀가루', quantity: 200, unit: 'g' },
      { name: '들깨가루', quantity: 4, unit: '큰술' },
      { name: '애호박', quantity: 0.5, unit: '개' },
    ],
    substitutes_json: null,
  },
  {
    id: 1,
    shared_post_id: null,
    food_name: '닭갈비',
    recipe_name: '매콤한 닭갈비',
    art: 'stirfry',
    image_url: null,
    estimated_cost: 8000,
    selected_at: '2026-08-14T10:40:00Z',
    recipe_json: {
      cook_time_minutes: 30,
      servings: 3,
      difficulty: '보통',
      steps: [
        '닭다리살을 손질해 양념에 30분 재운다.',
        '양배추와 고구마를 큼직하게 썬다.',
        '팬에 재료를 모두 넣고 센 불에 볶는다.',
        '떡과 깻잎을 넣고 5분 더 볶는다.',
      ],
    },
    ingredients_json: [
      { name: '닭다리살', quantity: 500, unit: 'g' },
      { name: '양배추', quantity: 0.25, unit: '통' },
      { name: '고구마', quantity: 1, unit: '개' },
      { name: '떡볶이떡', quantity: 100, unit: 'g' },
    ],
    substitutes_json: [
      { original: '닭다리살', substitute: '닭가슴살', reason: '지방을 줄이고 싶을 때' },
    ],
  },
]

// ---------------------------------------------------------
// 재료별 가격 (STEP 17~18) — 저장하지 않고 조회 시점에만 보여준다
//
// 쇼핑몰마다 값이 달라서 재료 하나에 여러 offer가 붙는다.
// 화면에서는 이 중 최저가와 최고가를 뽑아 보여준다.
// ---------------------------------------------------------
export const MOCK_PRICES = [
  {
    name: '돼지고기 앞다리살 200g',
    offers: [
      { shop: '쿠팡', price: 3980, link: '#' },
      { shop: '네이버쇼핑', price: 4350, link: '#' },
      { shop: '알리익스프레스', price: 5200, link: '#' },
    ],
  },
  {
    name: '신김치 300g',
    offers: [
      { shop: '네이버쇼핑', price: 2400, link: '#' },
      { shop: '쿠팡', price: 2890, link: '#' },
      { shop: '알리익스프레스', price: 4100, link: '#' },
    ],
  },
  {
    name: '두부 1모',
    offers: [
      { shop: '쿠팡', price: 1290, link: '#' },
      { shop: '네이버쇼핑', price: 1450, link: '#' },
    ],
  },
  {
    name: '대파 1대',
    offers: [
      { shop: '네이버쇼핑', price: 830, link: '#' },
      { shop: '쿠팡', price: 1100, link: '#' },
      { shop: '알리익스프레스', price: 2300, link: '#' },
    ],
  },
]

// ---------------------------------------------------------
// Ban 재료 (STEP 9) — user_bans 테이블
// ---------------------------------------------------------
export const MOCK_BANS = [
  { id: 1, ingredient_name: '오이', ban_type: 'DISLIKE', created_at: '2026-08-10T02:00:00Z' },
  { id: 2, ingredient_name: '땅콩', ban_type: 'ALLERGY', created_at: '2026-08-10T02:01:00Z' },
  { id: 3, ingredient_name: '고수', ban_type: 'DISLIKE', created_at: '2026-08-12T05:30:00Z' },
]

// ---------------------------------------------------------
// 게시판 (STEP 20~23) — posts / post_likes / comments
//
// meal_history_id 는 NOT NULL 이다. 모든 글은 작성자가 확정한 식사에서 출발한다.
// 아래 목업은 남이 쓴 글이므로, 여기 적힌 id 는 그 사람의 meal_histories 를 가리킨다
// (내 기록인 MOCK_HISTORIES 와 번호가 겹쳐 보이는 것은 목업이라 그런 것뿐이다).
// ---------------------------------------------------------
export const MOCK_POSTS = [
  {
    id: 3,
    meal_history_id: 3,
    nickname: '집밥러',
    title: '눈 오는 날엔 역시 김치찌개',
    content:
      '오늘 추천받은 대로 김치찌개 끓였는데 진짜 맛있었어요.\n' +
      '돼지고기 대신 참치캔 넣어도 괜찮다고 나와서 그렇게 했는데 국물이 더 깔끔하더라고요.\n\n' +
      '재료비도 만원 안 넘어서 부담 없었습니다. 추천해요!',
    food_name: '김치찌개',
    view_count: 128,
    like_count: 12,
    comment_count: 2,
    created_at: '2026-08-16T10:30:00Z',
  },
  {
    id: 2,
    meal_history_id: 2,
    nickname: '요리초보',
    title: '수제비 반죽 실패담...',
    content:
      '물을 한 번에 다 부어버려서 반죽이 완전 질어졌어요.\n' +
      '조금씩 넣으라는 이유가 있었네요. 다음엔 성공하겠습니다.',
    food_name: '들깨 수제비',
    view_count: 64,
    like_count: 5,
    comment_count: 1,
    created_at: '2026-08-14T12:10:00Z',
  },
  {
    id: 1,
    meal_history_id: 1,
    nickname: '닭갈비마스터',
    title: '닭갈비 양념 비율 공유합니다',
    content:
      '고추장 3 : 간장 1 : 설탕 1 : 다진마늘 1 비율이 제일 무난했어요.\n' +
      '매운 거 좋아하시면 고춧가루 한 스푼 추가하세요.',
    food_name: '닭갈비',
    view_count: 302,
    like_count: 34,
    comment_count: 3,
    created_at: '2026-08-11T11:00:00Z',
  },
]

// ---------------------------------------------------------
// 공지사항 — 운영자만 쓸 수 있는 글. posts 테이블과 분리해서 생각한다.
//
// 아직 DDL에 notices 테이블이 없다. 화면을 먼저 만들어두고 서버는 나중에 붙인다.
// 서버가 생기면 이 배열 모양 그대로 내려주면 된다.
//
//   pinned   상단 고정. 목록에서 항상 위로 올라온다
//   category 배지 문구. '점검' 만 경고색으로 칠한다
// ---------------------------------------------------------
export const MOCK_NOTICES = [
  {
    id: 5,
    category: '점검',
    pinned: true,
    title: '8월 25일(월) 새벽 서버 점검 안내 (02:00~04:00)',
    content:
      '추천 기능 개선을 위한 서버 점검이 예정되어 있습니다.\n' +
      '점검 시간 동안에는 로그인과 레시피 추천을 이용하실 수 없습니다.\n' +
      '작성 중인 글은 미리 저장해두시길 권해드립니다.',
    view_count: 1042,
    created_at: '2026-08-22T09:00:00Z',
  },
  {
    id: 4,
    category: '안내',
    pinned: true,
    title: '커뮤니티 이용 규칙 안내',
    content:
      '서로 존중하는 말투로 소통해주세요.\n' +
      '레시피는 자세히 공유해주실수록 좋아요.\n' +
      '링크 및 홍보성 글은 사전 안내 없이 삭제될 수 있습니다.\n' +
      '신고는 관리자 확인 후 조치됩니다.',
    view_count: 2317,
    created_at: '2026-08-01T00:00:00Z',
  },
  {
    id: 3,
    category: '업데이트',
    pinned: false,
    title: '못 먹는 재료 등록 기능이 추가되었습니다',
    content:
      '마이페이지에서 알레르기 재료와 싫어하는 재료를 등록할 수 있습니다.\n' +
      '알레르기로 등록한 재료는 추천에서 완전히 제외되고,\n' +
      '싫어하는 재료는 가능한 한 대체재료로 바꿔 제안합니다.',
    view_count: 486,
    created_at: '2026-08-20T05:30:00Z',
  },
  {
    id: 2,
    category: '이벤트',
    pinned: false,
    title: '이달의 레시피 공유 이벤트 (~8/31)',
    content:
      '직접 만든 요리 사진과 레시피를 커뮤니티에 올려주세요.\n' +
      '좋아요를 가장 많이 받은 세 분께 소정의 상품을 드립니다.\n' +
      '당첨자는 9월 첫째 주에 공지로 안내드립니다.',
    view_count: 731,
    created_at: '2026-08-12T02:00:00Z',
  },
  {
    id: 1,
    category: '안내',
    pinned: false,
    title: '가격 정보는 참고용입니다',
    content:
      '재료 가격은 네이버쇼핑·쿠팡·알리익스프레스에서 수집한 값입니다.\n' +
      '실제 결제 금액은 배송비와 판매자 사정에 따라 달라질 수 있습니다.\n' +
      '특히 해외배송 상품은 도착까지 시간이 오래 걸릴 수 있으니 확인해주세요.',
    view_count: 264,
    created_at: '2026-08-05T07:15:00Z',
  },
]

export const MOCK_COMMENTS = [
  {
    id: 1,
    post_id: 3,
    nickname: '요리초보',
    content: '참치캔 조합 좋네요! 저도 해봐야겠어요.',
    created_at: '2026-08-16T11:00:00Z',
  },
  {
    id: 2,
    post_id: 3,
    nickname: '닭갈비마스터',
    content: '김치를 먼저 볶는 게 포인트죠. 잘 하셨습니다.',
    created_at: '2026-08-16T12:20:00Z',
  },
]

// ---------------------------------------------------------
// 요리 레벨 (사이드바 하단 카드 · 상단바 프로필)
//
// 서버가 붙으면 GET /users/me 의 응답에 이 값들이 함께 실린다.
// exp / exp_next 를 퍼센트로 미리 계산해서 내려받지 않는다.
// 화면마다 필요한 표현(막대, "180 남음")이 달라서 원본 두 값을 그대로 두는 편이 낫다.
// ---------------------------------------------------------
export const MOCK_PROFILE = {
  level: 12,
  title: '요리 초보',
  exp: 320,
  exp_next: 500,
  streak_days: 7,
  recommend_count: 12,
  cook_count: 5,
}

// ---------------------------------------------------------
// 알림 (상단바 종 아이콘)
//
// read: false 인 건수가 종에 붙는 숫자가 된다.
// ---------------------------------------------------------
export const MOCK_NOTIFICATIONS = [
  {
    id: 3,
    icon: '🔥',
    title: '오늘의 추천이 도착했어요',
    body: '맑고 더운 날씨에 어울리는 메뉴 5가지를 골라뒀어요.',
    created_at: '2026-08-24T00:10:00Z',
    read: false,
  },
  {
    id: 2,
    icon: '💬',
    title: '작성하신 글에 댓글이 달렸어요',
    body: '"눈 오는 날엔 역시 김치찌개"에 새 댓글 2개',
    created_at: '2026-08-23T13:20:00Z',
    read: false,
  },
  {
    id: 1,
    icon: '🏆',
    title: '레벨이 올랐어요! Lv.12 요리 초보',
    body: '연속 방문 7일을 채워 보너스 경험치를 받았어요.',
    created_at: '2026-08-22T09:00:00Z',
    read: false,
  },
]

// ---------------------------------------------------------
// 검색 도우미
//
// suggestions 는 상단바·검색 카드에 깔리는 예시 검색어다.
// 앞의 두 개는 메뉴 이름, 뒤로 갈수록 문장형이라 "이렇게도 물어볼 수 있어요"를 보여준다.
// ---------------------------------------------------------
export const MOCK_SEARCH_SUGGESTIONS = [
  '김치찌개',
  '닭가슴살',
  '8천원 이하 파스타',
  '매운 국물요리',
  '냉장고에 계란이랑 대파 있어요',
  '10분 안에 되는 야식',
  '자취 첫 요리로 좋은 메뉴',
  '비 오는 날 어울리는 음식',
]

// ---------------------------------------------------------
// 예산 빠른 선택
//
// max: null 은 "예산 조건 없음"이다. 상한값이 아니라 필터를 끄는 선택지라
// 이때만 /budget 대신 /recommend 로 보낸다(BudgetPicker 참고).
// ---------------------------------------------------------
export const MOCK_BUDGET_PRESETS = [
  { key: 'UNDER_5K', label: '5천원 이하', max: 5000 },
  { key: 'UNDER_10K', label: '1만원 이하', max: 10000 },
  { key: 'UNDER_20K', label: '2만원 이하', max: 20000 },
  { key: 'ANY', label: '상관없음', max: null },
]

// ---------------------------------------------------------
// 메뉴 게임
//
// 세 가지 모두 "못 고르겠다"를 푸는 도구인데 성격이 다르다.
//   월드컵   후보를 둘씩 비교해 취향을 좁힌다 (시간이 좀 걸린다)
//   밸런스   질문 다섯 개로 조건을 만든다 (취향을 모를 때)
//   돌림판   그냥 운에 맡긴다 (제일 빠르다)
// ---------------------------------------------------------
export const MOCK_GAMES = [
  {
    key: 'worldcup',
    to: '/game/worldcup',
    emoji: '🏆',
    title: '음식 월드컵',
    desc: '두 메뉴 중 하나를 골라\n최종 우승 메뉴를 정해요!',
  },
  {
    key: 'balance',
    to: '/game/balance',
    emoji: '⚖️',
    title: '취향 밸런스',
    desc: '몇 가지 질문으로\n딱 맞는 메뉴를 추천!',
  },
  {
    key: 'roulette',
    to: '/roulette',
    emoji: '🎡',
    title: '랜덤 돌림판',
    desc: '운명의 돌림판을 돌려\n오늘의 메뉴를 결정해요!',
  },
]

// ---------------------------------------------------------
// 취향 밸런스 질문
//
// 선택지에 함수를 두지 않고 조건 값만 담는다. 서버가 내려주는 JSON으로
// 그대로 옮길 수 있어야 하기 때문이다. 점수 계산은 화면(BalancePage)이 한다.
//
//   tags     이 태그를 가진 음식에 +1
//   maxCost  이 값 이하면 +1     minCost  이 값 이상이면 +1
//   maxTime  이 시간 이하면 +1   minTime  이 시간 이상이면 +1
// ---------------------------------------------------------
export const MOCK_BALANCE_QUESTIONS = [
  {
    id: 1,
    question: '오늘 당기는 쪽은?',
    options: [
      { key: 'A', emoji: '🍲', label: '뜨끈한 국물', tags: ['국물'] },
      { key: 'B', emoji: '🔥', label: '불맛 나는 볶음', tags: ['볶음', '튀김'] },
    ],
  },
  {
    id: 2,
    question: '입맛은 어느 쪽?',
    options: [
      { key: 'A', emoji: '🌶️', label: '얼큰하고 매콤하게', tags: ['매콤', '얼큰'] },
      { key: 'B', emoji: '🥬', label: '순하고 담백하게', tags: ['건강', '시원한음식', '노불'] },
    ],
  },
  {
    id: 3,
    question: '밥이냐 면이냐',
    options: [
      { key: 'A', emoji: '🍚', label: '역시 밥 한 그릇', tags: ['덮밥', '해장'] },
      { key: 'B', emoji: '🍜', label: '면이 당기는 날', tags: ['면'] },
    ],
  },
  {
    id: 4,
    question: '주방에 서 있을 시간은?',
    options: [
      { key: 'A', emoji: '⏱️', label: '20분 안에 끝내기', maxTime: 20 },
      { key: 'B', emoji: '👨‍🍳', label: '시간 들여 제대로', minTime: 25 },
    ],
  },
  {
    id: 5,
    question: '오늘 지갑 사정은?',
    options: [
      { key: 'A', emoji: '🪙', label: '만원 아래로', maxCost: 8000 },
      { key: 'B', emoji: '💳', label: '오늘은 좀 쓴다', minCost: 8000 },
    ],
  },
]

// ---------------------------------------------------------
// 냉장고 재료로 찾기
//
// 냉장고에 있을 법한 재료를 분류별로 깔아두고 고르게 한다.
// 직접 입력도 되지만, 칩을 먼저 보여줘야 뭘 적어야 할지 감이 온다.
// ---------------------------------------------------------
export const MOCK_FRIDGE_GROUPS = [
  {
    key: 'VEGETABLE',
    label: '채소',
    items: ['대파', '양파', '감자', '애호박', '오이', '양배추', '고구마', '버섯', '토마토', '깻잎'],
  },
  {
    key: 'PROTEIN',
    label: '육류 · 해산물',
    items: ['돼지고기', '닭고기', '소고기', '참치캔', '연어', '어묵', '해물'],
  },
  {
    key: 'DAIRY',
    label: '유제품 · 계란',
    items: ['계란', '우유', '치즈', '생크림', '요거트', '버터'],
  },
  {
    key: 'ETC',
    label: '주식 · 그 외',
    items: ['밥', '밀가루', '소면', '스파게티면', '떡', '두부', '신김치', '콩나물', '바나나', '딸기'],
  },
]

/**
 * 음식별 주재료.
 *
 * MOCK_RECOMMENDATIONS 안에 필드로 넣지 않고 이름표로 뺐다.
 * 냉장고 찾기에서만 쓰는 값이라 추천 목록을 그리는 모든 화면이
 * 쓰지도 않는 배열을 함께 들고 다닐 이유가 없다.
 *
 * 여기 적는 재료 이름은 MOCK_FRIDGE_GROUPS 의 칩과 글자가 같아야 매칭된다.
 */
export const MOCK_FOOD_INGREDIENTS = {
  김치찌개: ['돼지고기', '신김치', '두부', '대파'],
  '들깨 수제비': ['밀가루', '애호박', '감자', '대파'],
  닭갈비: ['닭고기', '양배추', '고구마', '떡', '깻잎'],
  '간단 돈까스': ['돼지고기', '계란', '밀가루', '양배추'],
  된장찌개: ['두부', '애호박', '감자', '양파'],
  콩나물국밥: ['콩나물', '밥', '계란', '대파'],
  순두부찌개: ['두부', '계란', '대파', '해물'],
  '소고기 미역국': ['소고기', '대파'],
  마파두부: ['두부', '돼지고기', '대파', '양파'],
  간짜장: ['돼지고기', '양파', '감자', '소면'],
  '연어 사케동': ['연어', '밥', '깻잎'],
  '크림 파스타': ['스파게티면', '우유', '버섯', '생크림', '버터'],
  함박스테이크: ['소고기', '양파', '계란', '버터'],
  '채끝 스테이크': ['소고기', '버터', '버섯'],
  떡볶이: ['떡', '어묵', '대파', '계란'],
  충무김밥: ['밥', '어묵', '오이'],
  팟타이: ['소면', '계란', '닭고기', '양파'],
  '소고기 쌀국수': ['소고기', '소면', '양파', '깻잎'],
  '닭가슴살 샐러드': ['닭고기', '양배추', '토마토', '오이'],
  '리코타 치즈 샐러드': ['우유', '치즈', '토마토', '양배추'],
  '바나나 팬케이크': ['바나나', '계란', '우유', '버터'],
  '딸기 요거트볼': ['딸기', '요거트', '바나나'],
  '참치마요 덮밥': ['참치캔', '밥', '계란', '양파'],
  '전자레인지 계란찜': ['계란', '대파'],
  콩국수: ['소면', '오이', '두부'],
  물냉면: ['소면', '오이', '계란'],
  오이냉국: ['오이', '양파'],
  비빔국수: ['소면', '오이', '신김치', '계란'],
  자루소바: ['소면', '대파', '깻잎'],
}

// ---------------------------------------------------------
// AI 추천 기록
//
// 추천 결과 자체는 DB에 저장하지 않기로 했지만(§13), "언제 어떤 조건으로
// 무엇을 추천받았는지"는 남는다. 고른 메뉴(picked)가 있으면 그 건이
// meal_histories 로도 넘어간 것이다.
// ---------------------------------------------------------
export const MOCK_AI_LOGS = [
  {
    id: 5,
    source: '날씨 추천',
    condition: '서울특별시 · 맑음 28° · 못 먹는 재료 3건 제외',
    foods: ['콩국수', '물냉면', '오이냉국', '비빔국수', '자루소바'],
    picked: '콩국수',
    created_at: '2026-08-24T00:10:00Z',
  },
  {
    id: 4,
    source: '예산 추천',
    condition: '0원 ~ 8,000원',
    foods: ['콩나물국밥', '충무김밥', '참치마요 덮밥', '떡볶이'],
    picked: null,
    created_at: '2026-08-22T10:40:00Z',
  },
  {
    id: 3,
    source: '음식 월드컵',
    condition: '8강 · 우승 메뉴 1건',
    foods: ['닭갈비'],
    picked: '닭갈비',
    created_at: '2026-08-20T12:05:00Z',
  },
  {
    id: 2,
    source: '레시피 검색',
    condition: '"매운 국물요리"',
    foods: ['김치찌개', '순두부찌개', '부대찌개'],
    picked: '김치찌개',
    created_at: '2026-08-18T09:30:00Z',
  },
  {
    id: 1,
    source: '냉장고 재료',
    condition: '계란 · 대파 · 밥',
    foods: ['전자레인지 계란찜', '콩나물국밥', '참치마요 덮밥'],
    picked: null,
    created_at: '2026-08-16T13:15:00Z',
  },
]
