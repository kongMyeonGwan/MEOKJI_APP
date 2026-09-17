// 백엔드가 보낸 에러를 화면에서 쓰기 좋게 바꿔주는 함수.
//
// FastAPI 는 detail 을 두 가지 모양으로 보낸다.
//   1. 문자열 - 우리가 직접 낸 에러
//   2. 배열   - 입력값 검증 실패(422)
//
// 항상 { fieldErrors, message } 모양으로 돌려준다.

export function parseApiError(err) {
  let detail = null
  if (err && err.raw) {
    detail = err.raw.detail
  }

  // 검증 실패(422) 일 때는 입력칸별로 나눠준다
  if (Array.isArray(detail)) {
    const fieldErrors = {}

    for (let i = 0; i < detail.length; i++) {
      const item = detail[i]

      // loc 는 ["body", "nickname"] 모양이라 마지막 것이 칸 이름이다
      let loc = item.loc
      if (!loc) {
        loc = []
      }
      const field = loc[loc.length - 1]

      if (!field) {
        continue
      }

      // 한 칸에 에러가 여러 개면 첫 번째만 보여준다
      if (fieldErrors[field]) {
        continue
      }

      // Pydantic 이 앞에 붙이는 "Value error, " 를 떼어낸다
      const message = String(item.msg).replace(/^Value error,\s*/, '')
      fieldErrors[field] = message
    }

    return { fieldErrors: fieldErrors, message: '' }
  }

  // 문자열이면 그대로 쓴다
  if (typeof detail === 'string') {
    return { fieldErrors: {}, message: detail }
  }

  let message = '요청을 처리하지 못했습니다.'
  if (err && err.message) {
    message = err.message
  }

  return { fieldErrors: {}, message: message }
}
