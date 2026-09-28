// 게시판 API.
//
// ! 아직 만들지 않았다. 담당자가 채운다.
//
// 서버(backend-spring 의 controller/ReviewController)가 지금 501(미구현)을
// 돌려준다. 그래서 화면 대부분은 이 파일을 부르지 않고
// src/data/sampleBoard.js 의 하드코딩 데이터를 그린다
// (ReviewWritePage 만 이미 이 파일을 부른다).
//
// 만드는 순서
// -----------
//   1. 서버의 mapper/ReviewMapper 와 service/ReviewService 를 구현한다
//      (규칙은 docs/BOARD_SPEC.md 에 있다)
//   2. 맨 위 import 주석을 풀고, 아래 함수들을 client 호출로 바꾼다
//   3. 화면(CommunityPage · ReviewDetailPage · PopularPage · ReviewWritePage ·
//      PopularRecipes)에서 SAMPLE_* 대신 이 함수를 부른다
//   4. src/data/sampleBoard.js 를 지운다
//
// 함수 이름과 인자는 화면이 기대하는 모양이다. 바꾸면 화면도 같이 고쳐야 한다.

// import client from './client'

const NOT_READY = '게시판 기능은 아직 준비 중입니다.'

// 게시글 목록 한 페이지. 최신순.
// 배열이 아니라 { items, total, page, size, total_pages } 가 온다.
export async function getReviews(keyword, page) {
  // const params = { page: page || 1 }
  // if (keyword && keyword.trim()) {
  //   params.keyword = keyword.trim()
  // }
  // const response = await client.get('/api/reviews', { params: params })
  // return response.data
  throw new Error(NOT_READY)
}

// 게시글 상세 + 댓글
export async function getReview(reviewId) {
  throw new Error(NOT_READY)
}

// 글쓰기. 확정한 식사 기록 하나를 골라야 쓸 수 있다.
// 레시피랑 재료는 서버가 원본 기록에서 복사해온다.
export async function createReview(form) {
  throw new Error(NOT_READY)
}

// 좋아요 켜기 / 끄기. { liked, like_count } 가 온다.
// 토글이 아니라 원하는 상태를 보낸다. 그래야 따닥 눌러도 숫자가 안 어긋난다.
export async function setReviewLike(reviewId, liked) {
  throw new Error(NOT_READY)
}

// 댓글 작성
export async function addComment(reviewId, content) {
  throw new Error(NOT_READY)
}

// 글 수정. 제목이랑 본문만 바뀐다.
export async function updateReview(reviewId, form) {
  throw new Error(NOT_READY)
}

// 글 삭제. 내 글만 지울 수 있다.
export async function removeReview(reviewId) {
  throw new Error(NOT_READY)
}

// 사진 올리기 / 바꾸기. FormData 로 보내고
// Content-Type 을 multipart/form-data 로 덮어써야 한다.
export async function uploadReviewFile(reviewId, file) {
  throw new Error(NOT_READY)
}

// 사진 떼기
export async function removeReviewFile(reviewId) {
  throw new Error(NOT_READY)
}

// 사진을 실제로 가져올 주소를 만든다.
// 서버는 '/api/reviews/12/file' 처럼 앞부분 없이 주기 때문에
// <img src> 에 넣으려면 백엔드 주소를 앞에 붙여야 한다.
export function reviewFileUrl(path) {
  if (!path) {
    return ''
  }

  // let base = client.defaults.baseURL
  // if (!base) {
  //   base = ''
  // }
  // return base + path
  return path
}

// 조회수 +1
export async function countReviewView(reviewId) {
  throw new Error(NOT_READY)
}

// 댓글 수정. 내 댓글만 고칠 수 있다.
export async function updateComment(reviewId, commentId, content) {
  throw new Error(NOT_READY)
}

// 댓글 삭제. 내 댓글만 지울 수 있다.
export async function removeComment(reviewId, commentId) {
  throw new Error(NOT_READY)
}

// 인기 레시피. 고른 기간에 새로 받은 좋아요가 많은 순.
// start / end 는 'YYYY-MM-DD' 이고, 안 주면 최근 30일이다.
export async function getPopular(options) {
  throw new Error(NOT_READY)
}
