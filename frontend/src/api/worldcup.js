import client from './client'
import { authHeader } from '../utils/token'

// 음식 이상형 월드컵 우승 기록 · 명예의 전당 API.

// 우승 한 판을 남긴다. 로그인했을 때만 부른다.
// 실패해도 우승 화면은 그대로 보여야 해서 에러를 밖으로 던지지 않는다.
export const recordWorldcupWin = async ({ foodName, runnerUp, roundSize }) => {
  try {
    await client.post(
      '/api/games/worldcup/wins',
      { food_name: foodName, runner_up: runnerUp, round_size: roundSize },
      { headers: authHeader() },
    )
  } catch (err) {
    // 기록이 안 남아도 괜찮다
  }
}

// 우승 횟수 순위. 로그인 없이도 부를 수 있다.
// { items: [{ rank: 1, food_name: '김치찌개', wins: 5, finals: 7 }, ...], total: 12 }
export const getWorldcupRankings = async (limit = 5) => {
  const response = await client.get('/api/games/worldcup/rankings', { params: { limit } })
  return response.data
}
