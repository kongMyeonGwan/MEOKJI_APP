import client from './client'

/**
 * 날씨 API (STEP 12).
 * 좌표를 안 주면 서버가 종로2가 날씨로 대답한다(위치 권한 거부·미지원 시 fallback).
 */
export async function fetchWeather({ lat, lon } = {}) {
  const params = {}
  if (lat != null && lon != null) {
    params.lat = lat
    params.lon = lon
  }
  const { data } = await client.get('/api/weather', { params })
  return data
}
