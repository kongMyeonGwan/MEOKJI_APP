import client from './client'
import { authHeader } from '../utils/token'

// 재료 대체 챗봇 API. 로그인이 필요하다.
// 못 먹는 재료는 서버가 직접 읽으므로 화면에서 보내지 않는다.

// 질문 하나 보내기.
// message: 이번 질문, history: 이전 대화 [{ role: 'user' | 'assistant', content }] (최대 10개)
// 돌려주는 값: { answer }
export const askChat = async (message, history) => {
  const response = await client.post(
    '/api/chat',
    { message, history },
    { headers: authHeader() },
  )
  return response.data
}