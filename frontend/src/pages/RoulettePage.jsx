import Roulette from '../components/recommendation/Roulette'
import { GAME_KEYS } from '../api/games'
import { useGamePlayLog } from '../hooks/useGamePlayLog'
import '../styles/pages/ListPage.css'

/**
 * 음식 추천 룰렛 전용 화면.
 * 대시보드의 같은 카드를 넓게 펼친 것이다.
 *
 * 제목을 따로 두지 않는다. Roulette 카드가 이미 같은 제목을 달고 있어서
 * 화면에 "음식 추천 룰렛"이 두 번 보인다.
 *
 * 게임 기록은 대시보드가 아니라 이 화면에서만 남긴다. 대시보드에도 같은
 * 카드가 있는데, 거기서 세면 "룰렛을 하러 온 것"과 "홈을 연 것"이
 * 구분되지 않는다.
 */
export default function RoulettePage() {
  // 이 게임을 시작했다고 서버에 한 번 남긴다 (로그인했을 때만)
  useGamePlayLog(GAME_KEYS.ROULETTE)

  return (
    <div className="list-page list-page--narrow">
      <Roulette />
    </div>
  )
}
