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
