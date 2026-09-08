/**
 * 백엔드가 보낸 에러를 화면에 쓰기 좋은 모양으로 바꾼다.
 *
 * FastAPI 는 상황에 따라 두 가지 모양으로 detail 을 보낸다.
 *
 *   1) 문자열 — 우리가 직접 낸 에러
 *      { "detail": "이미 사용 중인 이메일입니다." }
 *
 *   2) 배열 — Pydantic 검증 실패(422). 어느 칸이 틀렸는지가 들어있다
 *      { "detail": [ { "loc": ["body", "nickname"], "msg": "..." } ] }
 *
 * 2번은 입력칸별로 쪼개서 각 칸 아래에 빨간 글씨로 보여줄 수 있게 만든다.
 *
 * 항상 이 모양으로 돌려준다.
 *   { fieldErrors: { nickname: '닉네임을 입력해주세요' }, message: '' }
 */
export function parseApiError(err) {
  // api/client.js 가 서버 응답 본문을 raw 에 담아준다
  const detail = err?.raw?.detail

  // --- 검증 실패(422): 입력칸별로 나눈다 ---
  if (Array.isArray(detail)) {
    const fieldErrors = {}

    for (const item of detail) {
      // loc 는 ["body", "nickname"] 처럼 생겼다. 마지막 칸이 필드 이름이다.
      const loc = item.loc ?? []
      const field = loc[loc.length - 1]

      if (!field) continue

      // 한 칸에 에러가 여러 개면 첫 번째만 보여준다.
      // 다 보여주면 입력칸 하나 아래에 빨간 글씨가 세 줄씩 쌓인다.
      if (fieldErrors[field]) continue

      // Pydantic 이 앞에 붙이는 "Value error, " 를 떼어낸다
      const message = String(item.msg).replace(/^Value error,\s*/, '')
      fieldErrors[field] = message
    }

    return { fieldErrors, message: '' }
  }

  // --- 문자열 detail, 또는 client.js 가 만든 네트워크 에러 ---
  if (typeof detail === 'string') {
    return { fieldErrors: {}, message: detail }
  }

  return {
    fieldErrors: {},
    message: err?.message ?? '요청을 처리하지 못했습니다.',
  }
}
