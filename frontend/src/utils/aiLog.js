// AI 추천 기록 - 언제, 어떤 조건으로, 무엇을 추천받았는지.
// 서버가 아니라 브라우저(localStorage)에 저장한다.
//
// 왜 서버가 아닌가 — 추천은 로그인 없이도 쓸 수 있다. 서버에 남기려면
// "누가" 를 알아야 하는데, 로그인 안 한 사람은 가릴 방법이 없다.
// 그리고 보여준 추천을 다 저장하면 쓸 일도 없는 행이 끝없이 쌓인다.
//
// 실제로 먹은 것은 서버에 남는다(api/histories). 이쪽은 "무엇을
// 추천받았고 그 중 무엇을 골랐는지" 를 돌아보는 용도다.
//
// utils/gameResult 와 짜임새가 거의 같다. 둘 다 localStorage 에
// 목록을 쌓고 개수를 제한한다
//
// 쓰는 법:
//   addLog('날씨 추천', '서울 · 맑음 28°', ['콩국수', '냉면'])
//   const logs = getLogs()

const STORAGE_KEY = 'meokji.ailogs'
const MAX_LOGS = 50 // 50개가 넘으면 오래된 것부터 버린다
// 게임 기록(20개)보다 많이 두는 이유 — 추천은 훨씬 자주 받는다.
// 화면을 열 때마다 한 건씩 쌓이므로 20개면 하루 만에 넘친다

// 저장된 기록 전부. 최신이 맨 앞이다.
//
// 없으면 빈 배열을 돌려준다. 부르는 쪽이 바로 .map 이나 .find 를
// 쓸 수 있다
export const getLogs = () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved ? JSON.parse(saved) : []
}

// export 가 없다. 이 파일 안에서만 쓴다 - 바깥에서 목록을 통째로
// 바꿀 수 있게 열어두면 50개 제한을 건너뛸 수 있다
const saveLogs = (logs) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(logs))
}

// 추천 기록을 하나 남긴다. 추천된 게 없으면 안 남긴다.
//
// 조건이 까다로워 0건이 나온 경우를 안 남기는 이유 — 돌아볼 값이 없다.
// 남기면 기록 목록이 빈 줄로 찬다
export const addLog = (source, condition, foods) => {
  if (foods.length === 0) {
    return
  }

  const log = {
    // 지금 시각(밀리초)을 번호로 쓴다. 서버가 주는 번호가 없다
    id: Date.now(),
    // 어디서 받은 추천인지. '날씨 추천' · '카테고리' 등
    source: source,
    // 서버가 만들어준 한 줄 ("서울 · 맑음 28° · 한식").
    // 없으면 빈 글자로 둔다 - null 이 화면에 "null" 로 보이지 않게
    condition: condition || '',
    // 추천받은 음식 이름들
    foods: foods,
    // 그 중 무엇을 골랐는지. 아직 안 골랐으면 null.
    // 아래 markPicked 가 나중에 채운다
    picked: null,
    created_at: new Date().toISOString(),
  }

  // 최신이 위로 오게 앞에 넣고, 50개까지만 남긴다
  saveLogs([log, ...getLogs()].slice(0, MAX_LOGS))
}

// "이 메뉴로 정했다" 고 표시한다. 그 메뉴가 들어 있는 가장 최근 기록 하나에만.
//
// 왜 하나에만 — 같은 음식이 여러 번 추천됐을 수 있다. 전부 표시하면
// 한 번 고른 것이 여러 건으로 보인다.
//
// !log.picked 를 같이 보는 이유 — 이미 고른 기록은 건드리지 않는다.
// 안 보면 가장 최근 기록에 계속 덮어쓰게 된다
export const markPicked = (foodName) => {
  const logs = getLogs()
  const found = logs.find((log) => !log.picked && log.foods.includes(foodName))
  if (found) {
    // found 는 logs 안의 객체를 가리킨다. 그래서 여기서 값을 바꾸면
    // logs 도 같이 바뀌고, 그 logs 를 그대로 저장하면 된다
    found.picked = foodName
    saveLogs(logs)
  }
  // 못 찾으면 아무것도 안 한다. 추천을 거치지 않고 검색으로 바로
  // 고른 경우라, 남길 기록이 없는 것이 맞다
}

// 기록 전부 지우기
export const clearLogs = () => {
  localStorage.removeItem(STORAGE_KEY)
}

// 추천 결과에서 음식 이름만 뽑는다
//   [{ food_name: '김치찌개', ... }] -> ['김치찌개']
//
// 이 파일에 둔 이유 — addLog 에 넘길 모양을 만드는 일이다.
// 화면마다 .map 을 적으면 같은 코드가 여러 곳에 생긴다
//
// 이름만 남기는 이유 — 추천 결과 전체를 저장하면 한 건이 커지고,
// 돌아볼 때 필요한 것은 "무엇이 나왔는지" 뿐이다
export const foodNames = (items) => {
  return items.map((item) => item.food_name)
}
