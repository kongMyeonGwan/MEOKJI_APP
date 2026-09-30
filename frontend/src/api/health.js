import client from './client'

// 백엔드랑 DB 가 살아있는지 확인한다
export const fetchHealth = async () => {
  try {
    const response = await client.get('/api/health')
    return response.data
  } catch (err) {
    // DB 가 죽었을 때(503)도 서버가 상태 정보를 보내준다
    if (err.response) {
      return err.response.data
    }
    throw err
  }
}
