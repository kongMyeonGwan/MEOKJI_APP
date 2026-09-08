import { createContext, useContext, useEffect, useState } from 'react'

/**
 * AI 추천 기록 — 언제 · 어떤 조건으로 · 무엇을 추천받았는지.
 *
 * 왜 서버가 아니라 브라우저에 저장하나
 * ------------------------------------
 * "추천 후보는 DB에 저장하지 않는다"가 이 프로젝트의 설계 원칙이고(§13),
 * 테이블도 7개로 고정되어 있다. 그런데 추천만 받고 아무것도 안 고른 날이
 * 훨씬 많은데 그 기록이 어디에도 안 남으면 "저번에 나왔던 그 메뉴"를
 * 다시 찾을 방법이 없다.
 *
 * 그래서 메뉴 **이름과 조건만** localStorage 에 남긴다.
 * 레시피 본문은 저장하지 않는다. 다시 열 때 AI 가 새로 만든다.
 *
 * 한계도 분명하다. 브라우저를 바꾸면 기록이 안 따라온다.
 * 여러 기기에서 보이게 하려면 테이블이 하나 더 필요하고, 그건 설계를
 * 바꾸는 결정이라 지금 하지 않는다.
 *
 * 쓰는 법
 * -------
 *   const { logs, add, markPicked } = useAiLog()
 */

const STORAGE_KEY = 'meokji.ailogs'

/** 남겨둘 최대 건수. 오래된 것부터 버린다 */
const MAX_LOGS = 50

const AiLogContext = createContext(null)

/**
 * localStorage 에 저장해둔 기록을 읽어온다.
 *
 * try/catch 로 감싼 이유: localStorage 에는 사용자가 직접 아무 값이나 넣을 수
 * 있고, 예전 버전이 다른 모양으로 저장해뒀을 수도 있다.
 * 그때 JSON.parse 가 에러를 내면서 앱 전체가 하얀 화면이 되면 안 된다.
 */
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

    // 모양이 맞는 것만 남긴다. 하나라도 깨져 있으면 목록이 통째로 안 그려진다.
    const logs = []
    for (const item of parsed) {
      if (item && typeof item === 'object' && Array.isArray(item.foods)) {
        logs.push(item)
      }
    }

    return logs
  } catch {
    // 형식이 깨진 값. 조용히 빈 목록으로 시작한다
    return []
  }
}

export function AiLogProvider({ children }) {
  // useState 에 함수를 넘기면 "첫 렌더에서 한 번만 실행" 이라는 뜻이다.
  const [logs, setLogs] = useState(readSavedLogs)

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(logs))
    } catch {
      // 저장 공간이 꽉 찼거나 사생활 보호 모드. 기록을 못 남길 뿐이라 넘어간다.
    }
  }, [logs])

  /**
   * 추천을 하나 남긴다.
   *
   *   add({ source: '날씨 추천', condition: '서울 · 맑음 28°', foods: ['콩국수'] })
   *
   * 후보가 없으면(0건) 남기지 않는다. 아무것도 못 받은 요청까지 목록에 쌓이면
   * 화면이 빈 줄로 가득 찬다.
   */
  function add({ source, condition, foods }) {
    if (!foods || foods.length === 0) {
      return
    }

    const log = {
      // Date.now() 는 밀리초라, 같은 화면에서 두 번 눌러도 값이 겹치지 않는다
      id: Date.now(),
      source,
      condition: condition ?? '',
      foods,
      picked: null,
      created_at: new Date().toISOString(),
    }

    // 최신이 위로 오게 앞에 넣고, 오래된 것부터 버린다
    setLogs((prev) => [log, ...prev].slice(0, MAX_LOGS))
  }

  /**
   * 그 메뉴로 정했다고 표시한다.
   *
   * 어느 추천에서 고른 건지 모르므로, 그 메뉴가 들어 있는 **가장 최근**
   * 기록에만 표시한다. 전부에 표시하면 지난달 기록까지 소급해서 바뀐다.
   */
  function markPicked(foodName) {
    setLogs((prev) => {
      const next = []
      let done = false

      for (const log of prev) {
        if (!done && !log.picked && log.foods.includes(foodName)) {
          next.push({ ...log, picked: foodName })
          done = true
        } else {
          next.push(log)
        }
      }

      return next
    })
  }

  /** 기록을 전부 비운다. (설정 화면에서 쓴다) */
  function clear() {
    setLogs([])
  }

  const value = {
    logs,
    count: logs.length,
    add,
    markPicked,
    clear,
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
