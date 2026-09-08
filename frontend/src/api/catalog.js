import client from './client'

/**
 * 화면이 쓰는 고정 목록 API.
 *
 * 카테고리 칩 · 룰렛 조각 · 게임 목록 · 밸런스 질문 · 냉장고 재료 칩 ·
 * 예시 검색어 · 예산 프리셋을 서버에서 받아온다.
 *
 * 예전에는 mocks/index.js 에 하드코딩되어 있었다.
 * 서버로 옮긴 이유는 백엔드(AI 프롬프트)와 프론트(화면)가 같은 목록을 봐야
 * 하기 때문이다. 냉장고 재료 칩의 글자가 서버의 재료 이름과 다르면
 * 아무것도 매칭되지 않는다.
 *
 * 로그인 없이도 부를 수 있다. Guest 도 대시보드를 쓴다.
 */

/** 대시보드가 처음 뜰 때 필요한 목록 전부. 한 번만 부르면 된다. */
export async function getCatalog() {
  const { data } = await client.get('/api/catalog')
  return data
}

/** 공지사항. 고정(pinned) 글이 위로 온다. */
export async function getNotices() {
  const { data } = await client.get('/api/catalog/notices')
  return data
}
