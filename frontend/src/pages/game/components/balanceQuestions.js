// 음식 취향 밸런스 게임 질문 16개.
//
// 서버(catalog.json)에는 질문이 5개뿐이라 16강을 만들 수 없어서 여기에 따로 둔다.
// 앞의 5개는 서버 것과 같고, 6번부터 새로 넣었다.
// 8강 · 4강을 고르면 이 중에서 무작위로 뽑는다 (순서는 아래 순서를 지킨다).
//
// 보기 하나의 모양
//   { key, emoji, label, keyword, minCost?, maxCost? }
//   label   : 화면에 보이는 글자
//   keyword : AI 추천에 보내는 짧은 낱말. 서버가 검색어를 150자까지만 받아서
//             16개 답을 label 그대로 이으면 넘친다. 음식의 이름 · 태그 · 재료와
//             겹치는 낱말을 골라야 추천 점수에 반영된다.
//   minCost / maxCost : 예산 조건 (있으면 금액 조건으로도 보낸다)

const BALANCE_QUESTIONS = [
  {
    id: 1,
    question: '오늘 당기는 쪽은?',
    options: [
      { key: 'A', emoji: '🍲', label: '뜨끈한 국물', keyword: '국물' },
      { key: 'B', emoji: '🔥', label: '불맛 나는 볶음', keyword: '볶음' },
    ],
  },
  {
    id: 2,
    question: '입맛은 어느 쪽?',
    options: [
      { key: 'A', emoji: '🌶️', label: '얼큰하고 매콤하게', keyword: '얼큰' },
      { key: 'B', emoji: '🥬', label: '순하고 담백하게', keyword: '시원한음식' },
    ],
  },
  {
    id: 3,
    question: '밥이냐 면이냐',
    options: [
      { key: 'A', emoji: '🍚', label: '역시 밥 한 그릇', keyword: '덮밥' },
      { key: 'B', emoji: '🍜', label: '면이 당기는 날', keyword: '면' },
    ],
  },
  {
    id: 4,
    question: '주방에 서 있을 시간은?',
    options: [
      { key: 'A', emoji: '⏱️', label: '20분 안에 끝내기', keyword: '간단' },
      { key: 'B', emoji: '👨‍🍳', label: '시간 들여 제대로', keyword: '전골' },
    ],
  },
  {
    id: 5,
    question: '오늘 지갑 사정은?',
    options: [
      { key: 'A', emoji: '🪙', label: '만원 아래로', keyword: '', maxCost: 8000 },
      { key: 'B', emoji: '💳', label: '오늘은 좀 쓴다', keyword: '', minCost: 8000 },
    ],
  },
  {
    id: 6,
    question: '고기를 고른다면?',
    options: [
      { key: 'A', emoji: '🐄', label: '든든한 소고기', keyword: '소고기' },
      { key: 'B', emoji: '🐖', label: '고소한 돼지고기', keyword: '돼지고기' },
    ],
  },
  {
    id: 7,
    question: '냄비 속은?',
    options: [
      { key: 'A', emoji: '🥘', label: '보글보글 찌개', keyword: '찌개' },
      { key: 'B', emoji: '🍵', label: '맑고 깊은 국탕', keyword: '국탕' },
    ],
  },
  {
    id: 8,
    question: '바삭함 vs 노릇함',
    options: [
      { key: 'A', emoji: '🍤', label: '바삭한 튀김', keyword: '튀김' },
      { key: 'B', emoji: '🍢', label: '노릇한 구이', keyword: '구이' },
    ],
  },
  {
    id: 9,
    question: '오늘 날씨엔?',
    options: [
      { key: 'A', emoji: '🧊', label: '시원한 냉국 · 냉면', keyword: '냉국 냉면' },
      { key: 'B', emoji: '🍂', label: '선선한 날 따끈하게', keyword: '선선한' },
    ],
  },
  {
    id: 10,
    question: '반찬으로 고른다면?',
    options: [
      { key: 'A', emoji: '🍖', label: '짭조름한 조림', keyword: '조림' },
      { key: 'B', emoji: '🥗', label: '새콤한 무침', keyword: '무침' },
    ],
  },
  {
    id: 11,
    question: '비 오는 날이라면?',
    options: [
      { key: 'A', emoji: '🥞', label: '지글지글 부침', keyword: '부침' },
      { key: 'B', emoji: '🍛', label: '한 그릇 덮밥', keyword: '덮밥' },
    ],
  },
  {
    id: 12,
    question: '꼭 들어갔으면 하는 건?',
    options: [
      { key: 'A', emoji: '🥚', label: '계란이 들어간', keyword: '계란' },
      { key: 'B', emoji: '🥔', label: '감자가 들어간', keyword: '감자' },
    ],
  },
  {
    id: 13,
    question: '바다 vs 산',
    options: [
      { key: 'A', emoji: '🦐', label: '오징어 · 새우 해물', keyword: '오징어 새우' },
      { key: 'B', emoji: '🍄', label: '향긋한 버섯', keyword: '버섯' },
    ],
  },
  {
    id: 14,
    question: '오늘 한 끼는?',
    options: [
      { key: 'A', emoji: '🍢', label: '가볍게 분식', keyword: '분식' },
      { key: 'B', emoji: '🍱', label: '제대로 한식', keyword: '한식' },
    ],
  },
  {
    id: 15,
    question: '외식 기분을 낸다면?',
    options: [
      { key: 'A', emoji: '🍣', label: '깔끔한 일식', keyword: '일식' },
      { key: 'B', emoji: '🥟', label: '든든한 중식', keyword: '중식' },
    ],
  },
  {
    id: 16,
    question: '양념은 어느 쪽?',
    options: [
      { key: 'A', emoji: '🧄', label: '마늘 듬뿍', keyword: '마늘' },
      { key: 'B', emoji: '🌰', label: '참깨로 고소하게', keyword: '참깨' },
    ],
  },
]

export default BALANCE_QUESTIONS
