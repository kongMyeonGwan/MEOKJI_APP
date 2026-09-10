import client from './client'

/**
 * 화면이 쓰는 고정 목록 API.
 *
 * 카테고리 칩 · 룰렛 조각 · 게임 목록 · 밸런스 질문 · 냉장고 재료 칩 ·
 * 예시 검색어 · 예산 프리셋을 서버에서 받아온다.
 *
 * 공지사항은 여기 없다. 운영하면서 계속 새로 쓰는 값이라 파일이 아니라
 * DB(NOTICES)에 있고, api/notices.js 가 담당한다.
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

/**
 * 시·도 · 시·군·구 목록. 회원가입 · 마이페이지의 주소 드롭다운이 쓴다.
 *
 * /api/catalog 에 안 묶여 있다. 250개가 넘어서 합쳐두면 대시보드를 열 때마다
 * 쓰지도 않는 목록을 같이 받게 된다.
 *
 *   [{ name: "서울특별시", sigungu: ["종로구", "중구", ...] }, ...]
 */
export async function getRegions() {
  const { data } = await client.get('/api/catalog/regions')
  return data
}
