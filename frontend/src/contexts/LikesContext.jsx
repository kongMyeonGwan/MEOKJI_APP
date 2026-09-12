import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { clearLikes, getLikes, toggleLike } from '../api/likes'
import { useAuth } from './AuthContext'

/**
 * 찜한 레시피 목록.
 *
 * 음식 카드의 하트, 상단바의 하트 개수, /liked 화면이 전부 같은 목록을 봐야 한다.
 * 화면마다 따로 들고 있으면 한 곳에서 찜했을 때 다른 곳이 안 바뀐다.
 * 그래서 여기 한 곳에 모아두고 필요한 화면이 꺼내 쓴다.
 *
 * 어디에 저장하나
 * ---------------
 * 서버(RECIPE_LIKES 표)다. 전에는 localStorage 였는데 두 가지가 문제였다.
 *
 *   기기를 바꾸면 안 따라온다
 *   한 브라우저를 여러 사람이 쓰면 앞사람 찜이 보인다  <- 저장 키에 회원 구분이 없었다
 *
 * 로그인해야 찜할 수 있다
 * ----------------------
 * 비로그인 상태에서 하트를 누르면 화면이 로그인으로 보낸다(FoodCard 등).
 * 그래서 '회원 없는 찜' 이라는 상태 자체가 없고, 합칠 것도 없다.
 * 여기서는 로그인하지 않았으면 빈 목록을 들고 있으면 된다.
 *
 * 쓰는 법
 * -------
 *   const { names, count, has, toggle, clear, ready } = useLikes()
 */

const LikesContext = createContext(null)

export function LikesProvider({ children }) {
  const { isAuthenticated, user } = useAuth()

  const [names, setNames] = useState([])

  // 서버에서 받아오기 전인지. 목록이 비었을 때 "없음" 과 "아직 안 옴" 을 가른다.
  // 이게 없으면 /liked 화면이 열리자마자 "찜한 레시피가 없어요" 를 잠깐 보여준다.
  const [ready, setReady] = useState(false)

  // 로그인 상태가 바뀔 때마다 다시 받아온다.
  //
  // user?.id 를 의존성에 넣는 이유
  // -----------------------------
  // 로그아웃했다가 다른 계정으로 들어오면 isAuthenticated 는 true -> false ->
  // true 로 돌아오지만, 중간을 놓치면 앞사람 목록이 그대로 남는다.
  // id 가 바뀌는 것을 보면 계정이 바뀐 것을 확실히 잡는다.
  useEffect(() => {
    let alive = true

    if (!isAuthenticated) {
      // 로그아웃 상태에서는 들고 있을 것이 없다. 앞사람 목록도 여기서 지워진다.
      setNames([])
      setReady(true)
      return
    }

    setReady(false)

    getLikes()
      .then((data) => {
        if (alive) setNames(data.names)
      })
      .catch(() => {
        // 못 받아왔다고 화면을 막지 않는다. 하트가 빈 채로 보일 뿐이고,
        // 누르면 그때 서버가 진짜 상태를 알려준다.
        if (alive) setNames([])
      })
      .finally(() => {
        if (alive) setReady(true)
      })

    return () => {
      alive = false
    }
  }, [isAuthenticated, user?.id])

  /**
   * 하트를 눌렀을 때. 이미 찜했으면 빼고, 아니면 넣는다.
   *
   * 먼저 화면을 바꾸고 서버에 보낸다(낙관적 반영). 하트는 누르자마자 칠해져야
   * 누른 것 같다. 실패하면 되돌린다.
   *
   * 로그인 확인은 부르는 쪽(FoodCard 등)이 한다. 여기서 하면 Context 가
   * 화면 이동까지 알아야 하는데, 그건 이 파일이 할 일이 아니다.
   */
  const toggle = useCallback(async (name) => {
    const before = names
    const liked = before.includes(name)

    // 방금 찜한 것이 목록 맨 앞에 오도록 앞쪽에 넣는다 (서버 정렬과 같다)
    setNames(liked ? before.filter((item) => item !== name) : [name, ...before])

    try {
      const result = await toggleLike(name)

      // 서버가 최종 상태를 알려준다. 탭 두 개에서 같은 하트를 눌렀을 때처럼
      // 화면이 먼저 바꾼 값과 다를 수 있어서, 서버 쪽을 따른다.
      setNames((current) => {
        const has = current.includes(name)
        if (result.liked === has) return current
        return result.liked
          ? [name, ...current.filter((item) => item !== name)]
          : current.filter((item) => item !== name)
      })
    } catch {
      setNames(before)
    }
  }, [names])

  /** 이 음식을 찜해뒀는지 확인한다. */
  const has = useCallback((name) => names.includes(name), [names])

  /** 찜을 전부 비운다. (설정 화면에서 쓴다) */
  const clear = useCallback(async () => {
    const before = names
    setNames([])

    try {
      await clearLikes()
    } catch {
      setNames(before)
    }
  }, [names])

  const value = {
    names,
    count: names.length,
    ready,
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
