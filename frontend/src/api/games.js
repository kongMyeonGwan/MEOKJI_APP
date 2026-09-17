import client from './client'

// 미니게임 기록 API.
// 게임 목록은 /api/catalog 에서 오고, 여기는 "누가 어떤 게임을 했는지" 를 남긴다.
// 끝난 판이 아니라 시작한 판을 센다 (도중에 나가는 일이 많아서).

// 게임 key. catalog.json 의 games[].key 와 같아야 한다.
export const GAME_KEYS = {
  WORLDCUP: 'worldcup',
  BALANCE: 'balance',
  ROULETTE: 'roulette',
}

// 게임을 시작했다고 서버에 알린다.
// 실패해도 게임은 그대로 진행되어야 해서 에러를 밖으로 안 던진다.
export async function recordGamePlay(gameKey) {
  try {
    const response = await client.post('/api/games/' + gameKey + '/plays')
    return response.data
  } catch (err) {
    return null
  }
}
