import { createContext, useContext, useEffect, useState } from 'react'
import { clearLikes, getLikes, toggleLike } from '../api/likes'
import { useAuth } from './AuthContext'

// 찜한 레시피 목록을 앱 전체에서 같이 쓰기 위한 Context.
//
// 음식 카드의 하트, 상단바의 하트 개수, /liked 화면이 전부 같은 목록을 봐야 한다.
// 화면마다 따로 들고 있으면 한 곳에서 찜했을 때 다른 곳이 안 바뀐다.
//
// 목록은 서버(RECIPE_LIKES 표)에 저장된다. 그래서 로그인해야 찜할 수 있고,
// 로그인 안 했으면 여기서는 빈 목록만 들고 있으면 된다.
//
// 쓰는 법:
//   const { names, count, has, toggle, clear, ready } = useLikes()

const LikesContext = createContext(null)

export function LikesProvider({ children }) {
  const { isAuthenticated, user } = useAuth()

  const [names, setNames] = useState([])

  // 서버에서 받아오기 전인지. 목록이 비었을 때 "없음" 이랑 "아직 안 옴" 을 구분한다.
  // 이게 없으면 /liked 화면이 열리자마자 "찜한 레시피가 없어요" 가 잠깐 보인다.
  const [ready, setReady] = useState(false)

  // 계정이 바뀌면 다시 받아와야 한다.
  // isAuthenticated 만 보면 다른 계정으로 바로 갈아탔을 때 앞사람 목록이 남는다.
  let userId = null
  if (user) {
    userId = user.id
  }

  useEffect(() => {
    let alive = true

    if (!isAuthenticated) {
      // 로그아웃 상태면 들고 있을 게 없다. 앞사람 목록도 여기서 지워진다.
      setNames([])
      setReady(true)
      return
    }

    setReady(false)

    getLikes()
      .then(function (data) {
        if (alive) {
          setNames(data.names)
        }
      })
      .catch(function () {
        // 못 받아와도 화면을 막지는 않는다. 하트가 빈 채로 보일 뿐이다.
        if (alive) {
          setNames([])
        }
      })
      .finally(function () {
        if (alive) {
          setReady(true)
        }
      })

    return function () {
      alive = false
    }
  }, [isAuthenticated, userId])

  // 하트를 눌렀을 때. 이미 찜했으면 빼고 아니면 넣는다.
  //
  // 서버에 보내기 전에 화면을 먼저 바꾼다. 하트는 누르자마자 칠해져야
  // 누른 것 같기 때문이다. 실패하면 되돌린다.
  //
  // 로그인 확인은 부르는 쪽(FoodCard 등)에서 한다.
  async function toggle(name) {
    const before = names
    const liked = before.includes(name)

    if (liked) {
      setNames(before.filter(function (item) {
        return item !== name
      }))
    } else {
      // 방금 찜한 것이 맨 앞에 오게 한다 (서버 정렬이랑 같다)
      setNames([name].concat(before))
    }

    try {
      const result = await toggleLike(name)

      // 서버가 알려준 최종 상태를 따른다.
      // 탭 두 개에서 같은 하트를 눌렀을 때 화면 값이랑 다를 수 있다.
      setNames(function (current) {
        const nowHas = current.includes(name)

        if (result.liked === nowHas) {
          return current
        }

        const without = current.filter(function (item) {
          return item !== name
        })

        if (result.liked) {
          return [name].concat(without)
        }
        return without
      })
    } catch (err) {
      setNames(before)
    }
  }

  // 이 음식을 찜해뒀는지 확인한다
  function has(name) {
    return names.includes(name)
  }

  // 찜을 전부 비운다 (설정 화면에서 쓴다)
  async function clear() {
    const before = names
    setNames([])

    try {
      await clearLikes()
    } catch (err) {
      setNames(before)
    }
  }

  const value = {
    names: names,
    count: names.length,
    ready: ready,
    has: has,
    toggle: toggle,
    clear: clear,
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
