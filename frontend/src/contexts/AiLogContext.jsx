import { createContext, useContext, useEffect, useState } from 'react'

// AI 추천 기록 - 언제, 어떤 조건으로, 무엇을 추천받았는지.
//
// 추천 후보는 DB 에 저장하지 않는 게 이 프로젝트 규칙이라 서버에 못 남긴다.
// 그래서 메뉴 이름이랑 조건만 localStorage 에 남긴다.
// 레시피 본문은 저장 안 하고 다시 열 때 AI 가 새로 만든다.
//
// 브라우저를 바꾸면 기록이 안 따라온다는 한계가 있다.
//
// 쓰는 법:
//   const { logs, add, markPicked } = useAiLog()

const STORAGE_KEY = 'meokji.ailogs'

// 남겨둘 최대 건수. 넘으면 오래된 것부터 버린다.
const MAX_LOGS = 50

const AiLogContext = createContext(null)

// localStorage 에 저장해둔 기록을 읽어온다.
// try/catch 로 감싼 이유는 값이 깨져 있을 때 JSON.parse 가 에러를 내면서
// 앱 전체가 하얀 화면이 되기 때문이다.
function readSavedLogs() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)

    if (!saved) {
      return []
    }

    const parsed = JSON.parse(saved)

    if (!Array.isArray(parsed)) {
      return []
    }

    // 모양이 맞는 것만 남긴다. 하나만 깨져도 목록이 통째로 안 그려진다.
    const logs = []
    for (let i = 0; i < parsed.length; i++) {
      const item = parsed[i]
      if (item && typeof item === 'object' && Array.isArray(item.foods)) {
        logs.push(item)
      }
    }

    return logs
  } catch (err) {
    // 형식이 깨진 값이면 그냥 빈 목록으로 시작한다
    return []
  }
}

export function AiLogProvider({ children }) {
  // useState 에 함수를 넘기면 처음 한 번만 실행된다
  const [logs, setLogs] = useState(readSavedLogs)

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(logs))
    } catch (err) {
      // 저장 공간이 꽉 찼거나 시크릿 모드. 기록을 못 남길 뿐이라 넘어간다.
    }
  }, [logs])

  // 추천을 하나 남긴다.
  //   add({ source: '날씨 추천', condition: '서울 · 맑음 28°', foods: ['콩국수'] })
  //
  // 추천된 게 하나도 없으면 안 남긴다. 빈 줄이 목록에 쌓이기 때문이다.
  function add(input) {
    if (!input.foods || input.foods.length === 0) {
      return
    }

    let condition = input.condition
    if (!condition) {
      condition = ''
    }

    const log = {
      // Date.now() 는 밀리초라서 두 번 눌러도 값이 안 겹친다
      id: Date.now(),
      source: input.source,
      condition: condition,
      foods: input.foods,
      picked: null,
      created_at: new Date().toISOString(),
    }

    // 최신이 위로 오게 앞에 넣고, 50개가 넘으면 뒤를 자른다
    setLogs(function (prev) {
      return [log].concat(prev).slice(0, MAX_LOGS)
    })
  }

  // 그 메뉴로 정했다고 표시한다.
  // 어느 추천에서 고른 건지 알 수 없어서 그 메뉴가 들어 있는 가장 최근
  // 기록에만 표시한다. 전부 표시하면 지난달 기록까지 바뀐다.
  function markPicked(foodName) {
    setLogs(function (prev) {
      const next = []
      let done = false

      for (let i = 0; i < prev.length; i++) {
        const log = prev[i]

        if (!done && !log.picked && log.foods.includes(foodName)) {
          next.push({
            id: log.id,
            source: log.source,
            condition: log.condition,
            foods: log.foods,
            picked: foodName,
            created_at: log.created_at,
          })
          done = true
        } else {
          next.push(log)
        }
      }

      return next
    })
  }

  // 기록을 전부 비운다 (설정 화면에서 쓴다)
  function clear() {
    setLogs([])
  }

  const value = {
    logs: logs,
    count: logs.length,
    add: add,
    markPicked: markPicked,
    clear: clear,
  }

  return <AiLogContext.Provider value={value}>{children}</AiLogContext.Provider>
}

export function useAiLog() {
  const context = useContext(AiLogContext)

  if (context === null) {
    throw new Error('useAiLog()는 <AiLogProvider> 안에서만 쓸 수 있습니다.')
  }

  return context
}
