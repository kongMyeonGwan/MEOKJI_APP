import client from './client'

// 화면이 쓰는 고정 목록 API.
// 카테고리 칩, 룰렛 조각, 게임 목록, 밸런스 질문, 예시 검색어, 예산 프리셋.
// 로그인 없이도 부를 수 있다.

// 첫 화면이 뜰 때 필요한 목록 전부. 한 번만 부르면 된다.
export async function getCatalog() {
  const response = await client.get('/api/catalog')
  return response.data
}

// 시/도, 시/군/구 목록. 회원가입이랑 마이페이지의 주소 선택에서 쓴다.
// 250개가 넘어서 /api/catalog 에 같이 안 넣고 따로 뒀다.
export async function getRegions() {
  const response = await client.get('/api/catalog/regions')
  return response.data
}
