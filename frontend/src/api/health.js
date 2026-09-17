import client from './client'

// 백엔드랑 DB 가 살아있는지 확인한다.
export async function fetchHealth() {
  try {
    const response = await client.get('/api/health')
    return response.data
  } catch (err) {
    // DB 가 죽었을 때 503 이 오는데 axios 는 이것도 에러로 던진다.
    // 내용은 들어있으니 그대로 돌려준다.
    if (err.raw) {
      return err.raw
    }
    throw err
  }
}
