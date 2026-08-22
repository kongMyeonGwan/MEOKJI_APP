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
export const MOCK_WEATHER = {
  city: '서울',
  temp: 28,
  feels_like: 30,
  humidity: 52,
  condition: 'CLEAR',
  condition_text: '맑음',
  comment: ['오늘은 맑고 더운 날씨예요 ☀️', '시원하고 가벼운 음식이 어때요?'],
}

// ---------------------------------------------------------
// AI 음식 추천 (STEP 13) — DB에 저장하지 않는 후보 목록
//
// estimated_cost 를 넓게 흩어놨다. 예산 필터가 실제로 걸러지는지
// 확인하려면 값이 다양해야 한다. (예: 8000원 입력 시 앞의 4건만 남음)
// ---------------------------------------------------------
export const MOCK_RECOMMENDATIONS = [
  {
    food_name: '김치찌개',
    art: 'stew',
    image_url: null,
    reason: '따뜻한 국물로 속을 든든하게 채워주는 한국인의 찌개',
    cook_time_minutes: 25,
    difficulty: '쉬움',
    estimated_cost: 7800,
    tags: ['국물', '한식'],
  },
  {
    food_name: '들깨 수제비',
    art: 'soup',
    image_url: null,
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
    reason: '매콤한 양념과 닭고기의 조화로 밥도둑이 따로 없어요',
    cook_time_minutes: 30,
    difficulty: '보통',
    estimated_cost: 8000,
    tags: ['매콤', '한식'],
  },
  {
    food_name: '콩나물국밥',
    art: 'noodle',
    image_url: null,
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
    reason: '재료비는 높지만 한 번 끓이면 여러 끼를 먹을 수 있어요',
    cook_time_minutes: 50,
    difficulty: '보통',
    estimated_cost: 15400,
    tags: ['국물', '보양'],
  },
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
// ---------------------------------------------------------
export const MOCK_HISTORIES = [
  {
    id: 3,
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
// ---------------------------------------------------------
export const MOCK_POSTS = [
  {
    id: 3,
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
