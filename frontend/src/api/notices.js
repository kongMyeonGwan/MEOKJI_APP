// 공지사항 API.
//
// TODO(게시판 담당): 아직 서버가 501(미구현)을 돌려준다.
//   1. 서버의 공지 기능을 만든다
//   2. 아래 import 주석을 풀고, 함수 안의 throw 를 client 호출로 바꾼다
//   3. pages/community/notice/ 의 화면들에서 SAMPLE_NOTICE* 대신 이 함수를 부른다

// import client from './client'
// import { authHeader } from '../utils/token'

// 여섯 함수가 같은 문장을 쓴다. 각자 적으면 한쪽만 고치게 된다.
//
// 왜 빈 함수가 아니라 throw 인가 — 아무것도 안 하고 빈 목록을 돌려주면
// 화면에서는 "공지가 없는 상태" 로 보인다. 아직 안 만들었다는 사실이
// 드러나지 않아서 다 만들었다고 착각할 수 있다
const NOT_READY = '공지사항 기능은 아직 준비 중입니다.'

// 작성 · 수정 · 삭제는 관리자만 된다. { headers: authHeader() } 를 같이 넘긴다.
// 목록과 조회수는 누구나 볼 수 있어서 넘기지 않아도 된다.

// 공지 목록 한 페이지. 고정 공지가 위, 그 다음 최신순.
//
// 채울 때
//   const response = await client.get('/api/notices', {
//     params: { page: options.page, size: options.size },
//   })
//   return response.data
//
// 고정 공지를 위로 올리는 정렬은 서버가 한다. 화면에서 정렬하면
// 한 페이지 안에서만 올라가서, 2페이지의 고정 공지는 2페이지에 남는다
export const getNotices = async (options) => {
  throw new Error(NOT_READY)
}

// 공지 하나. 상세 화면이 부른다.
export const getNotice = async (noticeId) => {
  throw new Error(NOT_READY)
}

// 조회수 +1
//
// 상세를 읽을 때 같이 안 올린다. 주소가 따로 있어서 화면이 올릴 시점을
// 정할 수 있다 - 관리자가 고치려고 열어도 조회수가 오르면 안 된다.
//
// 채울 때 POST 로 부른다. GET 으로 두면 브라우저가 미리 불러서
// 조회수가 멋대로 오른다
export const countNoticeView = async (noticeId) => {
  throw new Error(NOT_READY)
}

// 아래는 관리자만 쓸 수 있다
//
// 화면에서 버튼을 숨기는 것으로는 막히지 않는다. 요청을 직접 보내면
// 통하므로, 서버가 토큰의 권한을 보고 403 을 낸다. 아래 셋을 채울 때
// { headers: authHeader() } 를 빠뜨리면 로그인 자체가 안 된 것으로 보인다

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
