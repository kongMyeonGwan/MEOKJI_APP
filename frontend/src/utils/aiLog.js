// AI 추천 기록 - 언제, 어떤 조건으로, 무엇을 추천받았는지.
// 서버가 아니라 브라우저(localStorage)에 저장한다.
//
// 쓰는 법:
//   addLog('날씨 추천', '서울 · 맑음 28°', ['콩국수', '냉면'])
//   const logs = getLogs()

const STORAGE_KEY = 'meokji.ailogs'
const MAX_LOGS = 50 // 50개가 넘으면 오래된 것부터 버린다

// 저장된 기록 전부. 최신이 맨 앞이다.
export const getLogs = () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved ? JSON.parse(saved) : []
}

const saveLogs = (logs) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(logs))
}

// 추천 기록을 하나 남긴다. 추천된 게 없으면 안 남긴다.
export const addLog = (source, condition, foods) => {
  if (foods.length === 0) {
    return
  }

  const log = {
    id: Date.now(),
    source: source,
    condition: condition || '',
    foods: foods,
    picked: null,
    created_at: new Date().toISOString(),
  }

  // 최신이 위로 오게 앞에 넣고, 50개까지만 남긴다
  saveLogs([log, ...getLogs()].slice(0, MAX_LOGS))
}

// "이 메뉴로 정했다" 고 표시한다. 그 메뉴가 들어 있는 가장 최근 기록 하나에만.
export const markPicked = (foodName) => {
  const logs = getLogs()
  const found = logs.find((log) => !log.picked && log.foods.includes(foodName))
  if (found) {
    found.picked = foodName
    saveLogs(logs)
  }
}

// 기록 전부 지우기
export const clearLogs = () => {
  localStorage.removeItem(STORAGE_KEY)
}

// 추천 결과에서 음식 이름만 뽑는다
//   [{ food_name: '김치찌개', ... }] -> ['김치찌개']
export const foodNames = (items) => {
  return items.map((item) => item.food_name)
}
