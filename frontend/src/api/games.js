import client from './client'
import { authHeader } from '../utils/token'

// 미니게임 기록 API. "누가 어떤 게임을 했는지" 를 남긴다.
//
// 게임 자체는 브라우저에서 돌아간다. 월드컵 대진 · 룰렛 회전 · 밸런스
// 문항을 전부 화면이 처리하고, 서버에는 "했다" 는 사실만 남긴다.
//
// 고른 메뉴는 여기 안 보낸다. 그건 식사 기록이라 api/histories 로 간다.

// 게임 이름. 서버의 catalog.json 과 같아야 한다.
//
// 글자를 직접 적지 않고 상수로 모아둔 이유 — 화면 네 곳에서 쓴다.
// 'wordcup' 처럼 오타를 내면 서버가 404 를 주는데, 기록이 안 남는 것은
// 조용히 넘어가므로(아래 recordGamePlay) 알아채기 어렵다.
export const GAME_KEYS = {
  WORLDCUP: 'worldcup',
  BALANCE: 'balance',
  ROULETTE: 'roulette',
}

// 게임을 시작했다고 서버에 알린다.
// 실패해도 게임은 계속해야 해서 에러를 밖으로 던지지 않는다.
//
// 로그인 안 한 사람도 게임을 할 수 있다. 그때는 401 이 오지만
// 아래 catch 가 삼키므로 게임은 그대로 진행된다.
//
// 두 번째 인자가 null 인 이유 — POST 는 보낼 본문 자리가 있는데
// 보낼 값이 없다. 누가(토큰) 무엇을(주소) 하는지가 이미 다 드러나 있다.
// 자리를 비우면 세 번째 인자(헤더)가 본문으로 들어간다
export const recordGamePlay = async (gameKey) => {
  try {
    await client.post(`/api/games/${gameKey}/plays`, null, { headers: authHeader() })
  } catch (err) {
    // 기록이 안 남아도 괜찮다
    //
    // 비어 있는 catch 가 보통은 나쁜 모양이지만, 여기서는 일부러다.
    // 기록은 통계용이고, 그것 때문에 게임을 못 하게 만들 이유가 없다
  }
}
