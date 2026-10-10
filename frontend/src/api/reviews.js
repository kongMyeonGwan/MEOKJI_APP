// 게시판 API.
//
// TODO(게시판 담당): 아직 서버가 501(미구현)을 돌려준다.
//   1. 서버의 ReviewMapper · ReviewService 를 만든다 (규칙은 docs/BOARD_SPEC.md)
//   2. 아래 import 주석을 풀고, 함수 안의 throw 를 client 호출로 바꾼다
//      (getReviews 에 예시를 적어뒀다)
//   3. 화면에서 SAMPLE_* 가데이터 대신 이 함수를 부른다
//   4. src/data/sampleBoard.js 를 지운다

// import client from './client'
// import { authHeader } from '../utils/token'

// 열다섯 함수가 같은 문장을 쓴다. 각자 적으면 한쪽만 고치게 된다.
//
// 왜 빈 함수가 아니라 throw 인가 — 아무것도 안 하고 빈 목록을 돌려주면
// 화면에서는 "글이 없는 상태" 로 보인다. 아직 안 만들었다는 사실이
// 드러나지 않아서 다 만들었다고 착각할 수 있다.
//
// 지금 화면들은 src/data/sampleBoard.js 의 가데이터로 돌아간다.
// 서버가 채워지면 그 import 를 이 함수들로 바꾸고 파일을 지운다
const NOT_READY = '게시판 기능은 아직 준비 중입니다.'

// 게시글 목록 한 페이지. { items, total, page, size, total_pages } 가 온다.
//
// 로그인이 필요한 함수(글쓰기 · 수정 · 삭제 · 좋아요 · 댓글)는
// { headers: authHeader() } 를 같이 넘겨야 한다. 목록 · 상세 · 조회수는
// 게스트도 볼 수 있어서 넘기지 않아도 된다.
export const getReviews = async (keyword, page) => {
  // const response = await client.get('/api/reviews', {
  //   params: { keyword: keyword, page: page },
  // })
  // return response.data
  throw new Error(NOT_READY)
}

// 게시글 상세 + 댓글
export const getReview = async (reviewId) => {
  throw new Error(NOT_READY)
}

// 글쓰기. 식사 기록 하나를 골라야 쓸 수 있다.
//
// 자유게시판과 다른 점이 이것뿐이다. "실제로 먹어본 사람의 후기" 로
// 두려는 것이고, 한 기록으로는 한 번만 쓸 수 있다(두 번째는 409).
//
// 채울 때 meal_history_id 를 꼭 보낸다. 그 기록이 내 것인지는
// 서버가 확인한다 - 남의 기록 번호를 넣어 보낼 수 있어서다
export const createReview = async (form) => {
  throw new Error(NOT_READY)
}

// 좋아요 켜기 / 끄기. { liked, like_count } 가 온다.
//
// 찜(api/likes 의 toggleLike)과 다르다. 찜은 서버가 뒤집고, 이쪽은
// 화면이 켤지 끌지를 정해 보낸다 - 주소가 PUT 과 DELETE 로 나뉘어 있다.
//
// 뒤집기가 아니라서 같은 요청을 두 번 보내도 결과가 같다. 느린
// 연결에서 사용자가 두 번 눌러도 꺼진 채로 끝나지 않는다.
//
// 채울 때 liked 값에 따라 메서드를 가린다
//   liked ? client.put(...) : client.delete(...)
//
// like_count 는 서버가 다시 센 값이다. 화면이 1 을 더하거나 빼서
// 맞추면 다른 사람이 같은 시간에 누른 것과 어긋난다
export const setReviewLike = async (reviewId, liked) => {
  throw new Error(NOT_READY)
}

// 글 수정 (제목, 본문)
export const updateReview = async (reviewId, form) => {
  throw new Error(NOT_READY)
}

// 글 삭제
export const removeReview = async (reviewId) => {
  throw new Error(NOT_READY)
}

// 조회수 +1
export const countReviewView = async (reviewId) => {
  throw new Error(NOT_READY)
}

// 사진 올리기. FormData 로 보낸다.
//
// 파일은 JSON 으로 못 보낸다. 채울 때 api/freePosts 의
// uploadFreePostFile 과 똑같이 하면 된다 — Content-Type 을 직접
// 적지 않는 것이 중요하다(브라우저가 경계 문자열까지 붙여준다).
//
// 글을 먼저 만들고 그 번호로 올린다. 첨부가 글에 딸린 것이라
// 글 번호가 있어야 저장할 수 있다
export const uploadReviewFile = async (reviewId, file) => {
  throw new Error(NOT_READY)
}

// 사진 떼기
export const removeReviewFile = async (reviewId) => {
  throw new Error(NOT_READY)
}

// 사진 주소 만들기. 서버는 '/api/reviews/12/file' 처럼 주기 때문에
// 앞에 백엔드 주소를 붙여야 <img src> 에 쓸 수 있다.
//
// 안 붙이면 브라우저가 리액트 주소(5173)로 찾아가서 사진이 깨진다.
//
// 이 함수만 async 가 아니다. 서버를 부르지 않고 글자만 이어 붙인다.
//
// 지금은 받은 값을 그대로 돌려준다 — 가데이터의 주소가 이미 완성된
// 모양이라서다. 서버를 붙일 때 아래 주석 줄로 바꾼다
// (완성된 모양: api/freePosts 의 freePostFileUrl)
export const reviewFileUrl = (path) => {
  // 첨부가 없는 글은 path 가 null 이다. 빈 글자를 돌려주면
  // <img src=""> 가 되어 화면이 깨진 그림을 그리지 않는다
  if (!path) {
    return ''
  }
  // return client.defaults.baseURL + path
  return path
}

// 댓글 작성
export const addComment = async (reviewId, content) => {
  throw new Error(NOT_READY)
}

// 댓글 수정
export const updateComment = async (reviewId, commentId, content) => {
  throw new Error(NOT_READY)
}

// 댓글 삭제
export const removeComment = async (reviewId, commentId) => {
  throw new Error(NOT_READY)
}

// 인기 레시피. 기간 안에 좋아요를 많이 받은 순.
//
// 목록(getReviews)과 주소가 나뉘어 있다. 정렬 기준과 조건이 다르다 -
// 목록은 최신순에 검색어이고, 이쪽은 좋아요순에 기간이다.
//
// 페이징이 없다. "상위 몇 개" 라서 2페이지가 없다.
//
// 채울 때 start · end 를 안 넘겨도 되게 둔다. 서버가 기본 기간을
// 정하게 하면 화면마다 기준이 달라지지 않는다
export const getPopular = async (options) => {
  throw new Error(NOT_READY)
}
