import { createContext, useContext, useEffect, useState } from 'react'

/**
 * 찜한 레시피 목록.
 *
 * 음식 카드의 하트, 상단바의 하트 개수, /liked 화면이 전부 같은 목록을 봐야 한다.
 * 화면마다 따로 들고 있으면 한 곳에서 찜했을 때 다른 곳이 안 바뀐다.
 * 그래서 여기 한 곳에 모아두고 필요한 화면이 꺼내 쓴다.
 *
 * 무엇을 저장하나
 * ---------------
 * 음식 이름(food_name)만 저장한다. 추천 결과는 아직 DB 에 저장하지 않아서
 * 서버가 만들어준 id 가 없고, 화면들도 이미 이름으로 레시피를 연다(/recipe/김치찌개).
 *
 * 나중에 서버에 찜 테이블이 생기면 아래 toggle 안쪽만 API 호출로 바꾸면 된다.
 * 화면 코드는 하나도 안 고쳐도 된다 — 이게 Context 로 묶어둔 이유다.
 *
 * 쓰는 법
 * -------
 *   const { names, count, has, toggle, clear } = useLikes()
 */

const STORAGE_KEY = 'meokji.likes'

const LikesContext = createContext(null)

/**
 * localStorage 에 저장해둔 찜 목록을 읽어온다.
 *
 * try/catch 로 감싼 이유: localStorage 에는 사용자가 직접 아무 값이나 넣을 수 있고,
 * 예전 버전이 다른 모양으로 저장해뒀을 수도 있다.
 * 그때 JSON.parse 가 에러를 내면서 앱 전체가 하얀 화면이 되면 안 된다.
 */
function readSavedNames() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)

    if (!saved) {
      return []
    }

    const parsed = JSON.parse(saved)

    // 배열이 아니면 우리가 저장한 값이 아니다
    if (!Array.isArray(parsed)) {
      return []
    }

    // 문자열만 남긴다 (숫자나 객체가 섞여 있으면 화면이 깨진다)
    return parsed.filter((item) => typeof item === 'string')
  } catch {
    // 형식이 깨진 값. 조용히 빈 목록으로 시작한다
    return []
  }
}

export function LikesProvider({ children }) {
  // useState 에 함수를 넘기면 "첫 렌더에서 한 번만 실행" 이라는 뜻이다.
  // readSavedNames() 처럼 괄호를 붙이면 렌더할 때마다 localStorage 를 읽는다.
  const [names, setNames] = useState(readSavedNames)

  // names 가 바뀔 때마다 localStorage 에 다시 저장한다.
  // 두 번째 인자 [names] 가 "names 가 바뀌면 실행" 이라는 뜻이다.
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(names))
  }, [names])

  /** 이미 찜했으면 빼고, 안 찜했으면 넣는다. */
  function toggle(name) {
    setNames((prev) => {
      if (prev.includes(name)) {
        return prev.filter((item) => item !== name)
      }
      // 방금 찜한 것이 목록 맨 앞에 오도록 앞쪽에 넣는다
      return [name, ...prev]
    })
  }

  /** 이 음식을 찜해뒀는지 확인한다. */
  function has(name) {
    return names.includes(name)
  }

  /** 찜을 전부 비운다. (설정 화면에서 쓴다) */
  function clear() {
    setNames([])
  }

  const value = {
    names,
    count: names.length,
    has,
    toggle,
    clear,
  }

  return <LikesContext.Provider value={value}>{children}</LikesContext.Provider>
}

export function useLikes() {
  const context = useContext(LikesContext)

  if (context === null) {
    throw new Error('useLikes()는 <LikesProvider> 안에서만 쓸 수 있습니다.')
  }

  return context
}
