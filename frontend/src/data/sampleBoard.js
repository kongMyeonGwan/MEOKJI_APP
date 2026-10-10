// 게시판 · 공지사항 퍼블리싱용 임시 데이터.
//
// ! 이 파일은 담당자가 API 를 붙이면 통째로 지운다.
//
// 지금 백엔드의 게시판 · 공지 엔드포인트는 501(미구현)을 돌려준다.
// 그래도 화면 모양은 확인할 수 있어야 해서, 서버에서 받아올 값을
// 여기에 같은 모양으로 적어두고 화면이 이걸 그리게 해놨다.
//
// 필드 이름은 서버 스키마(backend 의 review/vo · notice/vo)와 똑같다.
// 그래서 나중에 이 배열 대신 API 응답을 넣기만 하면 화면은 안 고쳐도 된다.
//
// 이름을 맞추는 것이 이 파일의 핵심이다. 'title' 을 'subject' 로 적어두면
// 화면은 잘 돌아가는데, 서버를 붙이는 순간 전부 빈칸이 된다.
//
// 내용을 그럴듯하게 적어둔 이유도 있다. '테스트1' · 'aaa' 로 채우면
// 글자 수가 실제와 달라서, 목록에서 제목이 두 줄로 넘치는지 · 본문
// 미리보기가 어디서 잘리는지를 확인할 수 없다.
//
// 지우는 순서 (담당자용)
//   1. api/reviews · api/notices 의 함수를 실제 호출로 바꾼다
//   2. 화면에서 SAMPLE_* import 를 그 함수로 바꾼다
//   3. 이 파일을 지운다

// 첨부 사진 미리보기용 임시 그림. 서버가 생기면 file.url 에 서버 주소가 들어온다.
//
// data: 로 시작하는 주소는 "그림 파일을 글자로 적어둔 것" 이다.
// 파일을 따로 두지 않아도 <img src> 에 바로 넣을 수 있다 -
// 이 파일을 지울 때 같이 사라지므로 쓰레기가 안 남는다.
//
// SVG 로 적은 이유 — 글자로 적을 수 있고 짧다. PNG 를 글자로 바꾸면
// 수천 글자가 된다.
//
// %23 은 '#' 이다. 주소 안에서 # 은 "여기부터는 책갈피" 라는 뜻이라
// 색 코드로 쓰려면 바꿔 적어야 한다
export const SAMPLE_PHOTO =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='480' height='300'>" +
  "<rect width='480' height='300' fill='%23e8e2d6'/>" +
  "<circle cx='240' cy='130' r='56' fill='%23cfc4ae'/>" +
  "<rect x='150' y='196' width='180' height='14' rx='7' fill='%23cfc4ae'/></svg>"

// ===== 게시글 목록 (GET /api/reviews) =====
// 서버는 { items, total, page, size, total_pages } 모양으로 준다.
//
// 여기 배열은 그 중 items 에 해당한다. 화면이 페이징을 쓸 때는
// 숫자 네 칸도 흉내 내야 하지만, 지금은 한 페이지만 보여준다.
//
// 목록에는 본문 전체가 아니라 앞부분만 있어야 맞는다(서버가 잘라 준다).
// 여기서는 짧게 적어둔 것으로 대신한다
export const SAMPLE_REVIEWS = [
  {
    id: 1,
    title: '비 오는 날엔 역시 김치부침개죠',
    content:
      '퇴근길에 비가 와서 그냥 지나칠 수가 없었어요. 묵은지가 남아 있길래 바로 부쳤는데 반죽에 찬물을 쓰니까 진짜 바삭하더라고요. 다음엔 오징어도 넣어볼까 합니다.',
    food_name: '김치부침개',
    nickname: '비오는날엔',
    view_count: 128,
    like_count: 12,
    comment_count: 3,
    created_at: '2026-09-16T10:20:00Z',
    has_file: false,
  },
  {
    id: 2,
    title: '자취 3년차의 제육볶음 정착 레시피',
    content:
      '이것저것 해보다가 결국 여기로 돌아왔습니다. 고기는 앞다리살이 제일 무난하고, 양념에 배즙 대신 사과를 갈아 넣으면 단맛이 자연스러워요.',
    food_name: '제육볶음',
    nickname: '자취생김씨',
    view_count: 341,
    like_count: 27,
    comment_count: 8,
    created_at: '2026-09-15T12:05:00Z',
    has_file: false,
  },
  {
    id: 3,
    title: '콩국수 국물, 믹서기 성능이 전부더라고요',
    content:
      '콩을 아무리 잘 불려도 믹서기가 약하면 입에서 껄끄러워요. 저는 두 번 갈고 체에 한 번 걸러서 씁니다. 소금은 먹기 직전에 넣는 게 낫습니다.',
    food_name: '콩국수',
    nickname: '여름한정',
    view_count: 89,
    like_count: 9,
    comment_count: 2,
    created_at: '2026-09-14T08:40:00Z',
    has_file: false,
  },
  {
    id: 4,
    title: '된장찌개 애호박은 나중에 넣으세요',
    content:
      '처음부터 같이 끓이면 흐물흐물해져서 씹는 맛이 없어요. 된장 푼 국물이 한 번 끓고 나서 넣으면 모양도 살고 단맛도 남습니다.',
    food_name: '된장찌개',
    nickname: '된장한그릇',
    view_count: 56,
    like_count: 6,
    comment_count: 1,
    created_at: '2026-09-13T22:10:00Z',
    has_file: false,
  },
  {
    id: 5,
    title: '멸치볶음에 물엿 대신 올리고당 쓰면',
    content:
      '식었을 때 딱딱해지지 않아서 도시락 반찬으로 좋습니다. 대신 단맛이 약하니까 간장을 조금 줄이는 게 균형이 맞아요.',
    food_name: '멸치볶음',
    nickname: '도시락러',
    view_count: 73,
    like_count: 4,
    comment_count: 0,
    created_at: '2026-09-12T18:30:00Z',
    has_file: false,
  },
  {
    id: 6,
    title: '고등어구이는 무를 깔고 구우면 안 탑니다',
    content:
      '팬에 바로 올리면 껍질이 눌어붙어서 뒤집을 때 다 부서져요. 무를 도톰하게 썰어 깔고 그 위에 올리면 속까지 고르게 익습니다.',
    food_name: '고등어구이',
    nickname: '굽는사람',
    view_count: 61,
    like_count: 4,
    comment_count: 2,
    created_at: '2026-09-12T09:15:00Z',
    has_file: false,
  },
]

// ===== 게시글 상세 (GET /api/reviews/{id}) =====
//
// 목록과 칸이 다르다. 상세에만 있는 것 — comments(댓글 목록) ·
// file(첨부) · recipe_json(레시피) · is_mine · is_liked.
//
// is_mine 과 is_liked 는 로그인한 사람에 따라 달라지는 값이다.
// 서버가 토큰을 보고 채우므로 화면이 계산하지 않는다.
//
// 하나만 적어둔 이유 — 상세 화면은 한 번에 글 하나만 그린다.
// 어느 글을 눌러도 이것이 뜨지만 모양 확인에는 충분하다
// 목록 항목에 본문 전체 · 레시피 · 댓글이 더 붙는다.
export const SAMPLE_REVIEW_DETAIL = {
  id: 1,
  title: '비 오는 날엔 역시 김치부침개죠',
  content:
    '퇴근길에 비가 와서 그냥 지나칠 수가 없었어요.\n묵은지가 남아 있길래 바로 부쳤는데, 반죽에 찬물을 쓰니까 진짜 바삭하더라고요.\n\n다음엔 오징어도 넣어볼까 합니다. 혹시 해보신 분 계시면 팁 좀 주세요!',
  nickname: '비오는날엔',
  view_count: 128,
  like_count: 12,
  created_at: '2026-09-16T10:20:00Z',
  food_name: '김치부침개',
  meal_history_id: 101,

  // 글쓴이가 확정했을 때의 레시피. 서버가 원본 기록에서 복사해서 준다.
  recipe_json: {
    cook_time_minutes: 20,
    servings: 2,
    difficulty: '쉬움',
    steps: [
      '묵은지 200g 은 국물을 살짝 짜고 잘게 썹니다.',
      '부침가루 1컵에 찬물 3/4컵을 넣고 덩어리가 없게 풉니다.',
      '썬 김치와 김치국물 2큰술을 반죽에 넣고 섞습니다.',
      '팬에 기름을 넉넉히 두르고 센 불에서 앞뒤로 노릇하게 부칩니다.',
    ],
  },
  ingredients_json: [
    { name: '묵은지', quantity: 200, unit: 'g' },
    { name: '부침가루', quantity: 1, unit: '컵' },
    { name: '식용유', quantity: 3, unit: '큰술' },
  ],

  // 첨부 사진 한 장. 없는 모양을 보려면 null 로 바꾼다.
  file: {
    url: SAMPLE_PHOTO,
    name: '김치부침개_완성.jpg',
    size_text: '184 KB',
  },

  liked: false,
  is_mine: true, // false 로 바꾸면 남의 글 모양 (수정 · 삭제 · 첨부 버튼이 사라진다)
  comments: [
    {
      id: 11,
      nickname: '자취생김씨',
      content: '오징어 넣으면 물이 나와서 반죽을 좀 되게 잡으셔야 해요!',
      created_at: '2026-09-16T11:02:00Z',
      is_mine: true, // 내 댓글 모양(수정 · 삭제 버튼)을 볼 수 있게 하나만 true 로 뒀다
    },
    {
      id: 12,
      nickname: '여름한정',
      content: '찬물 팁 감사합니다. 오늘 저녁에 바로 해볼게요.',
      created_at: '2026-09-16T12:30:00Z',
      is_mine: false,
    },
    {
      id: 13,
      nickname: '도시락러',
      content: '묵은지 없으면 그냥 김치로도 되나요?',
      created_at: '2026-09-16T19:15:00Z',
      is_mine: false,
    },
  ],
}

// ===== 공지사항 (GET /api/notices) =====
//
// 게시글과 칸이 다르다. 공지에는 pinned(상단 고정)가 있고
// 좋아요 · 댓글 · 첨부가 없다 - 운영자가 알리는 글이라
// 사람들이 반응을 남기는 자리가 아니다.
//
// 고정 공지가 위에 오게 적어두었다. 서버도 그 순서로 준다
export const SAMPLE_NOTICES = [
  {
    id: 1,
    title: '[안내] 재료 시세 정보가 업데이트되었습니다',
    content:
      '한국농수산식품유통공사(KAMIS) 조사가격 기준으로 매일 아침 갱신됩니다.\n주말과 공휴일은 조사가 없어 직전 영업일 값이 표시될 수 있습니다.',
    view_count: 412,
    pinned: true,
    created_at: '2026-09-15T01:00:00Z',
  },
  {
    id: 2,
    title: '[점검] 9월 20일 새벽 서버 점검 예정',
    content:
      '9월 20일(일) 03:00 ~ 05:00 동안 서비스 이용이 어려울 수 있습니다.\n작업이 일찍 끝나면 점검 시간은 앞당겨질 수 있습니다.',
    view_count: 233,
    pinned: true,
    created_at: '2026-09-14T09:00:00Z',
  },
  {
    id: 3,
    title: '커뮤니티 이용 규칙 안내',
    content:
      '후기는 직접 만들어본 요리에 대해서만 작성해주세요.\n광고 · 비방 글은 사전 안내 없이 삭제될 수 있습니다.',
    view_count: 178,
    pinned: false,
    created_at: '2026-09-10T05:30:00Z',
  },
  {
    id: 4,
    title: '메뉴 게임에 밸런스 게임이 추가되었습니다',
    content:
      '질문 다섯 개에 답하면 그 조건에 맞는 메뉴를 AI 가 골라줍니다.\n메뉴 게임 화면에서 바로 해보실 수 있어요.',
    view_count: 95,
    pinned: false,
    created_at: '2026-09-05T07:00:00Z',
  },
]

// ===== 공지 상세 (GET /api/notices/{id}) =====
// 목록 항목과 칸이 같고 수정 시각만 더 붙는다.
export const SAMPLE_NOTICE_DETAIL = {
  id: 1,
  title: '[안내] 재료 시세 정보가 업데이트되었습니다',
  content:
    '한국농수산식품유통공사(KAMIS) 조사가격 기준으로 매일 아침 갱신됩니다.\n주말과 공휴일은 조사가 없어 직전 영업일 값이 표시될 수 있습니다.\n\n가격은 전국 평균 소매가이며, 지역을 고르면 그 지역 값으로 바뀝니다.\n조사되지 않는 재료는 가격 대신 "-" 로 표시됩니다.',
  view_count: 412,
  pinned: true,
  created_at: '2026-09-15T01:00:00Z',
  updated_at: '2026-09-15T01:00:00Z',
}

// ===== 인기 레시피 (GET /api/reviews/popular) =====
//
// 게시글 목록과 같은 모양이다. 같은 표를 좋아요순으로 뽑은 것이라
// 서버도 같은 VO 로 돌려준다.
//
// 다른 점은 페이징이 없다는 것이다. "상위 몇 개" 라서 2페이지가 없고,
// 응답도 { items, total, ... } 이 아니라 배열 그대로 온다
// 고른 기간에 새로 받은 좋아요가 많은 순.
export const SAMPLE_POPULAR = [
  {
    id: 2,
    food_name: '제육볶음',
    recipe_name: '자취 3년차 제육볶음',
    summary: '간단한 재료로 만드는 실패 없는 자취 요리!',
    nickname: '자취생김씨',
    like_count: 27,
    comment_count: 8,
    cook_time_minutes: 25,
    tags: ['볶음', '선선한', '한식'],
    estimated_cost: 5800,
    liked_at: '2026-09-15T12:05:00Z',
    art: 'stirfry',
    image_url: '/images/food/제육볶음.png',
  },
  {
    id: 1,
    food_name: '김치부침개',
    recipe_name: '바삭한 묵은지 김치부침개',
    summary: '바삭한 식감의 초간단 김치부침개',
    nickname: '비오는날엔',
    like_count: 12,
    comment_count: 3,
    cook_time_minutes: 20,
    tags: ['부침', '튀김', '한식'],
    estimated_cost: 2500,
    liked_at: '2026-09-16T10:20:00Z',
    art: 'pancake',
    image_url: '/images/food/김치부침개.png',
  },
  {
    id: 3,
    food_name: '콩국수',
    recipe_name: '고소한 콩국수',
    summary: '여름에 딱! 고소하고 시원한 콩국수',
    nickname: '여름한정',
    like_count: 9,
    comment_count: 2,
    cook_time_minutes: 30,
    tags: ['면', '시원한음식', '여름메뉴', '한식'],
    estimated_cost: 3600,
    liked_at: '2026-09-14T08:40:00Z',
    art: 'noodle',
    image_url: '/images/food/콩국수.png',
  },
  {
    id: 4,
    food_name: '된장찌개',
    recipe_name: '구수한 애호박 된장찌개',
    summary: '애호박을 늦게 넣어 식감을 살린 된장찌개',
    nickname: '된장한그릇',
    like_count: 6,
    comment_count: 1,
    cook_time_minutes: 25,
    tags: ['국물', '한식'],
    estimated_cost: 3100,
    liked_at: '2026-09-13T22:10:00Z',
    art: 'stew',
    image_url: '/images/food/된장찌개.png',
  },
  {
    id: 5,
    food_name: '멸치볶음',
    recipe_name: '도시락용 꽈리고추 멸치볶음',
    summary: '아이들도 좋아하는 국민 밑반찬',
    nickname: '도시락러',
    like_count: 4,
    comment_count: 0,
    cook_time_minutes: 15,
    tags: ['볶음', '선선한', '한식'],
    estimated_cost: 2400,
    liked_at: '2026-09-12T18:30:00Z',
    art: 'stirfry',
    image_url: '/images/food/멸치볶음.png',
  },
  {
    id: 6,
    food_name: '고등어구이',
    recipe_name: '무 깔고 구운 고등어구이',
    summary: '껍질까지 바삭하게 구운 고등어',
    nickname: '굽는사람',
    like_count: 4,
    comment_count: 2,
    cook_time_minutes: 20,
    tags: ['구이', '선선한', '한식'],
    estimated_cost: 5200,
    liked_at: '2026-09-12T09:15:00Z',
    art: 'donburi',
    image_url: '/images/food/고등어구이.png',
  },
]
