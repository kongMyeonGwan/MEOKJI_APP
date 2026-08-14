import client from './client'

/** 백엔드 + DB 상태 조회. DB 장애 시 503이지만 body는 동일한 형태다. */
export async function fetchHealth() {
  try {
    const { data } = await client.get('/api/health')
    return data
  } catch (err) {
    // 503(degraded)도 axios는 에러로 던지므로 body를 살려서 반환한다
    if (err.raw) return err.raw
    throw err
  }
}
