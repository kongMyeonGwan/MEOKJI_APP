// 에러를 화면에 보여줄 문장 하나로 바꾼다.
//
// 쓰는 법:
//   try {
//     await login(...)
//   } catch (err) {
//     setError(getErrorMessage(err))
//   }
//
// axios 에러(err) 안에 들어 있는 것
//   err.response         서버가 보낸 응답. 서버가 꺼져 있으면 없다(undefined).
//   err.response.status  상태 코드 (400, 401, 404, 409 ...)
//   err.response.data    서버가 보낸 내용. 우리 서버는 { detail: '에러 문장' } 으로 보낸다.
export const getErrorMessage = (err) => {
  // 1. 서버가 응답을 보내준 경우 → 서버가 적어준 문장을 쓴다
  if (err.response) {
    const detail = err.response.data.detail

    // 보통은 문장 하나다. 예) "이미 가입된 이메일입니다."
    if (typeof detail === 'string') {
      return detail
    }

    // 입력값 검사에 실패하면 배열로 온다. 첫 번째 문장만 보여준다.
    // 예) [{ msg: "닉네임을 입력해주세요" }, ...]
    if (Array.isArray(detail) && detail.length > 0) {
      return detail[0].msg
    }

    return '요청을 처리하지 못했습니다.'
  }

  // 2. 서버가 응답을 안 한 경우 (백엔드를 안 켰거나, 30초가 지남)
  if (err.request) {
    return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.'
  }

  // 3. 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
  return err.message
}
