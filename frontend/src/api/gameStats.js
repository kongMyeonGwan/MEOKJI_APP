import client from './client'

// 메뉴 게임 통계 API. 로그인 없이도 부를 수 있다.

// 전체 회원의 게임별 플레이 횟수.
// { items: [{ game_key: 'worldcup', game_type: 'A', plays: 120 }, ...], total: 245 }
export const getGameStats = async () => {
  const response = await client.get('/api/games/stats')
  return response.data
}
