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

const NOT_READY = '게시판 기능은 아직 준비 중입니다.'

// 게시글 목록 한 페이지. { items, total, page, size, totalPages } 가 온다.
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
export const createReview = async (form) => {
  throw new Error(NOT_READY)
}

// 좋아요 켜기 / 끄기. { liked, likeCount } 가 온다.
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
export const uploadReviewFile = async (reviewId, file) => {
  throw new Error(NOT_READY)
}

// 사진 떼기
export const removeReviewFile = async (reviewId) => {
  throw new Error(NOT_READY)
}

// 사진 주소 만들기. 서버는 '/api/reviews/12/file' 처럼 주기 때문에
// 앞에 백엔드 주소를 붙여야 <img src> 에 쓸 수 있다.
export const reviewFileUrl = (path) => {
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
export const getPopular = async (options) => {
  throw new Error(NOT_READY)
}
