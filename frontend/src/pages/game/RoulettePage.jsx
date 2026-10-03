import { useEffect } from 'react'
import Roulette from './components/Roulette'
import { GAME_KEYS, recordGamePlay } from '../../api/games'
import { getToken } from '../../utils/token'

// 음식 추천 룰렛 화면
export default function RoulettePage() {
  // 화면이 열리면 "룰렛 게임을 했다" 고 서버에 기록한다 (로그인했을 때만)
  useEffect(() => {
    if (getToken()) {
      recordGamePlay(GAME_KEYS.ROULETTE)
    }
  }, [])

  return (
    <div className="list-page list-page--narrow">
      <Roulette />
    </div>
  )
}
