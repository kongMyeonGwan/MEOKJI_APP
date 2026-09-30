import client from './client'

// 미니게임 기록 API. "누가 어떤 게임을 했는지" 를 남긴다.

// 게임 이름. 서버의 catalog.json 과 같아야 한다.
export const GAME_KEYS = {
  WORLDCUP: 'worldcup',
  BALANCE: 'balance',
  ROULETTE: 'roulette',
}

// 게임을 시작했다고 서버에 알린다.
// 실패해도 게임은 계속해야 해서 에러를 밖으로 던지지 않는다.
export const recordGamePlay = async (gameKey) => {
  try {
    await client.post(`/api/games/${gameKey}/plays`)
  } catch (err) {
    // 기록이 안 남아도 괜찮다
  }
}
