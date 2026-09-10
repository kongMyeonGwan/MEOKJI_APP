import client from './client'

/**
 * 미니게임 기록 API.
 *
 * 게임 목록(무엇을 그릴지)은 /api/catalog 에서 온다. 여기는 "누가 어떤 게임을
 * 언제 시작했는가"를 남기는 쪽이다.
 *
 * 끝난 판이 아니라 시작한 판을 센다. 세 게임 모두 도중에 나가는 일이 흔해서
 * (월드컵은 일곱 번, 밸런스는 질문 다섯 개를 지나야 끝난다), 끝난 것만 세면
 * "이 게임을 얼마나 열어보는가"를 알 수 없다.
 *
 * 로그인이 필요하다. GAME_HISTORIES.USER_ID 가 NOT NULL 이라 Guest의 판은
 * 남길 자리가 없다. 그래서 화면은 로그인했을 때만 부르고, Guest도 게임
 * 자체는 그대로 할 수 있다.
 */

/** 화면의 게임 key. catalog.json 의 games[].key 와 같아야 한다. */
export const GAME_KEYS = {
  WORLDCUP: 'worldcup',
  BALANCE: 'balance',
  ROULETTE: 'roulette',
}

/**
 * 게임을 시작했다고 서버에 알린다.
 *
 * 실패해도 게임은 그대로 진행된다. 기록이 안 남았다고 화면에 에러를 띄우면,
 * 놀러 온 사람에게 아무 상관 없는 사정을 알리는 꼴이 된다.
 * 그래서 이 함수는 예외를 밖으로 던지지 않는다.
 */
export async function recordGamePlay(gameKey) {
  try {
    const { data } = await client.post(`/api/games/${gameKey}/plays`)
    return data
  } catch {
    return null
  }
}
