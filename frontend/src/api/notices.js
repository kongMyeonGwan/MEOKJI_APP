// 공지사항 API.
//
// ! 아직 만들지 않았다. 담당자가 채운다.
//
// 백엔드(backend/app/routers/notices.py)가 지금 501(미구현)을 돌려준다.
// 그래서 화면은 이 파일을 부르지 않고 src/data/sampleBoard.js 의
// 하드코딩 데이터를 그린다.
//
// 만드는 순서
// -----------
//   1. 백엔드 routers/notices.py 와 services/notice_service.py 를 구현한다
//   2. 맨 위 import 주석을 풀고, 아래 함수들을 client 호출로 바꾼다
//   3. CommunityPage 에서 SAMPLE_NOTICES 대신 이 함수를 부른다

// import client from './client'

const NOT_READY = '공지사항 기능은 아직 준비 중입니다.'

// 공지 목록 한 페이지. 고정 공지가 위, 그 다음 최신순.
// 배열이 아니라 { items, total, page, size, total_pages } 가 온다.
// 정렬은 서버가 한다. 고정이 위로 오는 건 화면 취향이 아니라 공지의 규칙이다.
export async function getNotices(options) {
  throw new Error(NOT_READY)
}

// 조회수 +1. 올라간 값 { view_count } 가 온다.
// 목록을 불러올 때 같이 올리면 화면만 열어도 전부 올라가서 따로 뒀다.
export async function countNoticeView(noticeId) {
  throw new Error(NOT_READY)
}

// --- 아래는 관리자만 쓸 수 있다 ---
// 화면에서도 버튼을 숨기지만 진짜로 막는 건 서버다.

// 공지 작성. 작성자는 서버가 토큰에서 꺼내 넣는다.
export async function createNotice(form) {
  throw new Error(NOT_READY)
}

// 공지 수정. PUT 이라서 세 항목을 전부 보낸다.
export async function updateNotice(noticeId, form) {
  throw new Error(NOT_READY)
}

// 공지 삭제. 서버는 진짜로 지우지 않고 use_yn 을 'N' 으로 바꾼다.
export async function deleteNotice(noticeId) {
  throw new Error(NOT_READY)
}
