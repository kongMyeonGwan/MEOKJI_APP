// 공지사항 API.
//
// TODO(게시판 담당): 아직 서버가 501(미구현)을 돌려준다.
//   1. 서버의 공지 기능을 만든다
//   2. 아래 import 주석을 풀고, 함수 안의 throw 를 client 호출로 바꾼다
//   3. pages/community/notice/ 의 화면들에서 SAMPLE_NOTICE* 대신 이 함수를 부른다

// import client from './client'
// 
const NOT_READY = '공지사항 기능은 아직 준비 중입니다.'

// 작성 · 수정 · 삭제는 관리자만 된다. 목록과 조회수는 누구나 볼 수 있다.
// 토큰은 client.js 의 인터셉터가 알아서 붙이므로 여기서 신경 쓰지 않아도 된다.

// 공지 목록 한 페이지. 고정 공지가 위, 그 다음 최신순.
export const getNotices = async (options) => {
  throw new Error(NOT_READY)
}

// 공지 하나. 상세 화면이 부른다.
export const getNotice = async (noticeId) => {
  throw new Error(NOT_READY)
}

// 조회수 +1
export const countNoticeView = async (noticeId) => {
  throw new Error(NOT_READY)
}

// 아래는 관리자만 쓸 수 있다

// 공지 작성
export const createNotice = async (form) => {
  throw new Error(NOT_READY)
}

// 공지 수정
export const updateNotice = async (noticeId, form) => {
  throw new Error(NOT_READY)
}

// 공지 삭제
export const deleteNotice = async (noticeId) => {
  throw new Error(NOT_READY)
}
