// 게임으로 고른 메뉴 - 언제, 어떤 게임에서, 무엇이 뽑혔는지.
//
// 서버가 아니라 브라우저(localStorage)에 저장한다. 게임 API(/api/games/{key}/plays)는
// "이 게임을 몇 번 했다" 만 세고 무엇이 뽑혔는지는 남기지 않기 때문이다.
// 나중에 GAME_HISTORIES 에 결과 칸이 생기면 서버 저장으로 옮기면 된다.
//
// 로그인한 회원마다 따로 저장한다 (저장 이름에 회원 번호를 붙인다).
// 같은 브라우저로 다른 사람이 로그인해도 앞 사람의 결과가 보이지 않는다.
// 로그인 안 했으면 저장하지 않고, 꺼내면 늘 비어 있다.
//
// 쓰는 법:
//   saveResult('worldcup', '음식 월드컵', '김치찌개', '/recipe/김치찌개')
//   const today = getTodayResult()   // 오늘 것만. 없으면 null
//
// link 를 같이 받는 이유는 게임마다 결과의 성격이 달라서다. 월드컵·밸런스는
// 음식 하나라 레시피로 보내면 되는데, 룰렛은 분류(한식 · 중식)를 뽑기 때문에
// 레시피가 아니라 그 분류의 추천 목록으로 보내야 한다.

import { getSavedUser } from './token'

const STORAGE_PREFIX = 'meokji.gameResults' // 실제 이름은 'meokji.gameResults.{회원번호}'
const MAX_RESULTS = 20 // 20개가 넘으면 오래된 것부터 버린다

// 지금 로그인한 회원의 저장 이름. 로그인 안 했으면 null
const storageKey = () => {
  const user = getSavedUser()
  if (!user || user.id == null) {
    return null
  }
  return `${STORAGE_PREFIX}.${user.id}`
}

// 저장된 결과 전부. 최신이 맨 앞이다. 로그인 안 했으면 빈 목록
export const getResults = () => {
  const key = storageKey()
  if (!key) {
    return []
  }
  const saved = localStorage.getItem(key)
  return saved ? JSON.parse(saved) : []
}

const saveResults = (results) => {
  const key = storageKey()
  if (!key) {
    return
  }
  localStorage.setItem(key, JSON.stringify(results))
}

// 오늘 날짜를 'YYYY-MM-DD' 로. 날짜 비교에만 쓴다
const todayKey = () => {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

// 게임 결과를 하나 남긴다. 게임이 끝나면 각 게임 화면이 부른다.
// 로그인 안 했으면 아무것도 하지 않는다.
export const saveResult = (gameKey, gameTitle, foodName, link) => {
  if (!foodName || !storageKey()) {
    return
  }

  const result = {
    id: Date.now(),
    gameKey: gameKey,
    gameTitle: gameTitle,
    foodName: foodName,
    link: link,
    day: todayKey(),
    createdAt: new Date().toISOString(),
  }

  saveResults([result, ...getResults()].slice(0, MAX_RESULTS))
}

// 오늘 뽑은 결과 중 가장 최근 것. 오늘 게임을 안 했으면 null
export const getTodayResult = () => {
  const today = todayKey()
  const found = getResults().find((result) => result.day === today)
  return found || null
}

// 지금 회원의 결과 전부 지우기
export const clearResults = () => {
  const key = storageKey()
  if (key) {
    localStorage.removeItem(key)
  }
}
