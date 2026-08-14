/**
 * 백엔드 에러를 화면에 쓸 수 있는 형태로 변환한다.
 *
 * FastAPI는 상황에 따라 두 가지 모양으로 detail을 준다.
 *   - HTTPException  : detail 이 문자열       (예: 409 "이미 사용 중인 이메일입니다.")
 *   - 검증 실패(422) : detail 이 배열         [{ loc: ['body','nickname'], msg: '...' }]
 *
 * 배열인 경우 필드별로 쪼개서 각 입력칸 아래에 표시할 수 있게 한다.
 */
export function parseApiError(err) {
  const raw = err?.raw?.detail

  // 422 검증 오류 -> { fieldErrors: { nickname: '...' }, message: '' }
  if (Array.isArray(raw)) {
    const fieldErrors = {}
    for (const item of raw) {
      const field = item.loc?.[item.loc.length - 1]
      if (field && !fieldErrors[field]) {
        // Pydantic이 붙이는 'Value error, ' 접두사를 제거
        fieldErrors[field] = String(item.msg).replace(/^Value error,\s*/, '')
      }
    }
    return { fieldErrors, message: '' }
  }

  // 409 등 문자열 detail, 또는 client.js가 만든 네트워크 에러 메시지
  return {
    fieldErrors: {},
    message: typeof raw === 'string' ? raw : (err?.message ?? '요청을 처리하지 못했습니다.'),
  }
}
