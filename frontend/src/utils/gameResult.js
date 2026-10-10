// 게임으로 고른 메뉴 - 언제, 어떤 게임에서, 무엇이 뽑혔는지.
//
// 서버가 아니라 브라우저(localStorage)에 저장한다. 게임 API(/api/games/{key}/plays)는
// "이 게임을 몇 번 했다" 만 세고 무엇이 뽑혔는지는 남기지 않기 때문이다.
// 나중에 GAME_HISTORIES 에 결과 칸이 생기면 서버 저장으로 옮기면 된다.
//
// 쓰는 법:
//   saveResult('worldcup', '음식 월드컵', '김치찌개', '/recipe/김치찌개')
//   const today = getTodayResult()   // 오늘 것만. 없으면 null
//
// link 를 같이 받는 이유는 게임마다 결과의 성격이 달라서다. 월드컵·밸런스는
// 음식 하나라 레시피로 보내면 되는데, 룰렛은 분류(한식 · 중식)를 뽑기 때문에
// 레시피가 아니라 그 분류의 추천 목록으로 보내야 한다.

const STORAGE_KEY = 'meokji.gameResults'
const MAX_RESULTS = 20 // 20개가 넘으면 오래된 것부터 버린다
// 한계를 두는 이유 — localStorage 는 도메인당 5MB 쯤이 한계다.
// 안 버리면 몇 년 쓴 사람의 저장소가 게임 기록으로 찬다.
//
// 20개로 둔 이유 — 화면이 쓰는 것은 "오늘 뽑은 것" 하나뿐이다.
// 나머지는 나중에 기록 화면을 만들 때를 위한 여유분이다

// 저장된 결과 전부. 최신이 맨 앞이다.
//
// 없으면 null 이 아니라 빈 배열을 돌려준다. 부르는 쪽이 바로
// .find 나 전개(...)를 쓸 수 있다
export const getResults = () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved ? JSON.parse(saved) : []
}

// export 가 없다. 이 파일 안에서만 쓴다 - 바깥에서 목록을 통째로
// 바꿀 수 있게 열어두면, 저장 규칙(20개 제한)을 건너뛸 수 있다
const saveResults = (results) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(results))
}

// 오늘 날짜를 'YYYY-MM-DD' 로. 날짜 비교에만 쓴다
//
// utils/format 의 toDateKey 와 하는 일이 같다. 그쪽을 쓰지 않은 이유는
// 이 파일이 format 을 가져오지 않아서인데, 둘이 따로 있는 모양이다.
//
// 글자로 견주는 이유 — Date 끼리는 === 로 비교할 수 없다(다른 객체다).
// 'YYYY-MM-DD' 는 자리가 고정이라 글자로 비교해도 날짜 비교가 된다
const todayKey = () => {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

// 게임 결과를 하나 남긴다. 게임이 끝나면 각 게임 화면이 부른다.
export const saveResult = (gameKey, gameTitle, foodName, link) => {
  // 뽑힌 것이 없으면 남기지 않는다. 게임을 중간에 그만둔 경우다
  if (!foodName) {
    return
  }

  const result = {
    // 지금 시각(밀리초)을 번호로 쓴다. 서버가 주는 번호가 없고,
    // 같은 밀리초에 두 번 저장할 일이 없어서 겹치지 않는다
    id: Date.now(),
    game_key: gameKey,
    game_title: gameTitle,
    food_name: foodName,
    link: link,
    // 날짜만 따로 둔다. created_at 에서 떼어낼 수도 있지만,
    // 아래 getTodayResult 가 비교할 때마다 떼어내야 한다
    day: todayKey(),
    created_at: new Date().toISOString(),
  }

  // 새것을 앞에 붙이고 20개까지만 남긴다.
  // [새것, ...기존] 으로 적으면 최신이 맨 앞이 된다
  saveResults([result, ...getResults()].slice(0, MAX_RESULTS))
}

// 오늘 뽑은 결과 중 가장 최근 것. 오늘 게임을 안 했으면 null
//
// 게임 화면 위쪽의 "오늘 뽑은 메뉴" 카드가 쓴다. 어제 것을 보여주면
// 오늘 또 돌려야 하는지 헷갈린다.
//
// find 는 첫 번째로 맞는 것을 돌려준다. 목록이 최신순이라
// 그것이 곧 "오늘 중 가장 최근" 이다
export const getTodayResult = () => {
  const today = todayKey()
  const found = getResults().find((result) => result.day === today)
  // find 는 못 찾으면 undefined 를 준다. null 로 바꿔 돌려주는 이유 —
  // 화면이 === null 로 비교하는 자리가 있어서 모양을 맞춘다
  return found || null
}

// 결과 전부 지우기
//
// 설정 화면의 "기록 비우기" 가 부른다. 빈 배열을 저장하지 않고
// removeItem 으로 칸 자체를 없앤다 - 위 getResults 가 없을 때
// 빈 배열을 주므로 결과는 같고, 저장소에 쓰레기가 안 남는다
export const clearResults = () => {
  localStorage.removeItem(STORAGE_KEY)
}
