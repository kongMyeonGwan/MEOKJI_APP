import client from './client'

// 화면이 쓰는 고정 목록 API. 로그인 없이도 부를 수 있다.

// 카테고리, 룰렛, 게임 목록, 밸런스 질문, 예시 검색어, 예산 칩
export const getCatalog = async () => {
  const response = await client.get('/api/catalog')
  return response.data
}

// 시/도, 시/군/구 목록 (주소 선택에서 쓴다)
export const getRegions = async () => {
  const response = await client.get('/api/catalog/regions')
  return response.data
}

// 고른 시/군/구에 딸린 읍/면/동 목록. ['사직동', '삼청동', ...] 이 온다.
// 전국 동이 3,500개라 목록 API 에 다 싣지 않고 고른 뒤에 따로 받는다.
export const getDongs = async (sido, sigungu) => {
  const response = await client.get('/api/catalog/regions/dong', { params: { sido: sido, sigungu: sigungu } })
  return response.data
}

// 내 위치(위도, 경도)에서 가장 가까운 시/도, 시/군/구, 읍/면/동. { sido, sigungu, dong } 이 온다.
export const getNearestRegion = async (lat, lon) => {
  const response = await client.get('/api/catalog/regions/nearest', { params: { lat: lat, lon: lon } })
  return response.data
}
