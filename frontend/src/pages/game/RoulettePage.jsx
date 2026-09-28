import { useEffect, useRef } from 'react'
import Roulette from '../../components/recommendation/Roulette'
import { GAME_KEYS, recordGamePlay } from '../../api/games'
import { useAuth } from '../../contexts/AuthContext'
import '../../styles/pages/ListPage.css'

// 음식 추천 룰렛 화면. 첫 화면에 있는 카드를 넓게 펼친 것이다.
//
// 제목을 따로 안 뒀다. Roulette 카드가 이미 같은 제목을 달고 있어서
// 화면에 "음식 추천 룰렛" 이 두 번 보인다.
//
// 게임 기록은 첫 화면이 아니라 여기서만 남긴다.
// 첫 화면에서 세면 "룰렛을 하러 온 것" 이랑 "홈을 연 것" 이 구분이 안 된다.
export default function RoulettePage() {
  const { isAuthenticated } = useAuth()

  // 두 번 기록되지 않게 막는다.
  // 개발 모드에서는 리액트가 useEffect 를 일부러 두 번 실행한다.
  const logged = useRef(false)

  useEffect(() => {
    if (!isAuthenticated || logged.current) {
      return
    }

    logged.current = true

    // 실패해도 조용히 넘어간다. 기록이 안 남았다고 에러를 띄울 이유가 없다.
    recordGamePlay(GAME_KEYS.ROULETTE)
  }, [isAuthenticated])

  return (
    <div className="list-page list-page--narrow">
      <Roulette />
    </div>
  )
}
