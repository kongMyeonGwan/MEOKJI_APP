import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

/**
 * 찜한 레시피.
 *
 * 카드의 하트, 상단바의 하트, /liked 화면이 같은 목록을 봐야 해서
 * 화면마다 들고 있지 않고 여기 한 곳에 모은다.
 *
 * 저장 단위는 food_name 이다. 추천 결과에는 아직 서버가 만든 id가 없고
 * (추천은 DB에 저장하지 않는다), 화면들이 이미 이름으로 레시피를 연다
 * (/recipe/:foodName). 서버에 찜 테이블이 생기면 id 기반으로 바꾸면서
 * 이 파일의 add/remove 만 API 호출로 교체하면 된다.
 */

const STORAGE_KEY = 'meokji.likes'

const LikesContext = createContext(null)

function readSaved() {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    return Array.isArray(raw) ? raw.filter((v) => typeof v === 'string') : []
  } catch {
    // 다른 버전이 남긴 값이거나 사용자가 직접 건드린 경우. 조용히 비우고 시작한다
    return []
  }
}

export function LikesProvider({ children }) {
  // 최근에 찜한 것이 앞에 오도록 앞쪽에 넣는다
  const [names, setNames] = useState(readSaved)

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(names))
  }, [names])

  const toggle = useCallback((name) => {
    setNames((prev) =>
      prev.includes(name) ? prev.filter((n) => n !== name) : [name, ...prev]
    )
  }, [])

  const clear = useCallback(() => setNames([]), [])

  const value = useMemo(
    () => ({
      names,
      count: names.length,
      has: (name) => names.includes(name),
      toggle,
      clear,
    }),
    [names, toggle, clear]
  )

  return <LikesContext.Provider value={value}>{children}</LikesContext.Provider>
}

export function useLikes() {
  const ctx = useContext(LikesContext)
  if (ctx === null) {
    throw new Error('useLikes()는 <LikesProvider> 안에서만 쓸 수 있습니다.')
  }
  return ctx
}
