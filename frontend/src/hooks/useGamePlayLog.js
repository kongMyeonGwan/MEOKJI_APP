import { useEffect, useRef } from 'react'
import { recordGamePlay } from '../api/games'
import { useAuth } from '../contexts/AuthContext'

/**
 * 게임 화면이 열릴 때 "이 게임을 시작했다"를 서버에 한 번 남긴다.
 *
 * 게임 페이지 세 곳(월드컵 · 밸런스 · 돌림판)이 같은 일을 한다.
 * 세 번 적는 대신 여기 한 번 적어두고 각자 한 줄로 부른다.
 *
 *   useGamePlayLog(GAME_KEYS.WORLDCUP)
 *
 * 왜 ref 로 한 번만 막는가
 * -----------------------
 * React 18 이후 개발 모드(StrictMode)는 effect 를 일부러 두 번 실행한다.
 * 정리(cleanup)를 제대로 썼는지 확인시켜 주는 장치인데, 그대로 두면
 * 개발 중에 게임 기록이 매번 두 줄씩 쌓인다. 실제 사용자 수가 두 배로
 * 보이는 통계가 되므로 막는다.
 *
 * 로그인 안 했으면 아무것도 안 한다. GAME_HISTORIES.USER_ID 가 NOT NULL 이라
 * Guest 의 판은 남길 자리가 없다. 게임 자체는 Guest 도 그대로 할 수 있다.
 */
export function useGamePlayLog(gameKey) {
  const { isAuthenticated } = useAuth()
  const logged = useRef(false)

  useEffect(() => {
    if (!isAuthenticated || logged.current) return

    logged.current = true

    // 실패해도 조용히 넘어간다(recordGamePlay 가 예외를 삼킨다).
    // 기록이 안 남았다고 놀러 온 사람에게 에러를 띄울 이유가 없다.
    recordGamePlay(gameKey)
  }, [gameKey, isAuthenticated])
}
