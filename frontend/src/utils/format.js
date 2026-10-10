// 화면에 보여줄 때 쓰는 포맷 함수들
//
// 왜 한 곳에 모았나 — 날짜와 금액을 화면마다 다르게 적으면 한 화면에
// "2026.08.16" 과 "2026-08-16" 이 같이 나온다. 모양을 한 번만 정해두면
// 바꿀 때도 한 곳만 고친다.
//
// 전부 값을 받아 글자를 돌려주기만 한다. 화면도 서버도 모르는 함수라
// 어디서든 부를 수 있고, 테스트하기도 쉽다.
//
// 값이 없으면 '-' 를 돌려주는 함수가 여럿이다. 빈 글자를 주면 화면에
// 자리가 비어 보여서 "안 불러와진 것" 처럼 읽힌다

// 한 자리 숫자 앞에 0 붙이기. 8 -> "08"
//
// export 가 없다. 이 파일 안에서만 쓴다 - 날짜를 만드는 함수 넷이
// 같이 쓰는 작은 도구라, 밖에서 부를 일이 없다
//
// 자리를 맞추는 이유 — "2026.8.6" 과 "2026.08.16" 이 섞이면
// 목록에서 날짜 칸의 폭이 들쭉날쭉해진다
const pad = (n) => {
  return String(n).padStart(2, '0')
}

// 8500 -> "8,500원"
//
// null 과 undefined 를 따로 가리는 이유 — if (!value) 로 적으면
// 0 도 걸린다. 0원은 "모른다" 가 아니라 "공짜" 라서 구별해야 한다
//
// toLocaleString 이 쉼표를 넣어준다. 자리 수를 세어 직접 넣는 코드를
// 짜면 틀릴 자리가 생긴다
export const formatPrice = (value) => {
  if (value === null || value === undefined) {
    return '-'
  }
  return `${value.toLocaleString()}원`
}

// 7200 -> "7천원대", 15400 -> "1만5천원대"
//
// 정확한 금액 대신 대략을 보여주는 자리에 쓴다. 추천 카드처럼
// "이 정도면 되겠다" 만 알면 되는 곳이다 - 재료비는 시세에 따라
// 움직이므로 "8,427원" 으로 적으면 더 정확해 보이기만 한다.
//
// 위 formatPrice 와 달리 null 을 돌려준다. 화면이 이 값이 없으면
// 그 자리를 아예 안 그리기 때문이다
export const priceBand = (value) => {
  // 여기는 if (!value) 로 충분하다. 0원짜리 메뉴는 없다
  if (!value) {
    return null
  }

  // 만 단위와 천 단위를 따로 구한다.
  //   15400 -> man 1, chun 5
  //   % 10000 으로 만 단위를 떼어낸 뒤 1000 으로 나눈다
  const man = Math.floor(value / 10000)
  const chun = Math.floor((value % 10000) / 1000)

  if (man === 0) {
    return `${chun}천원대`
  }
  if (chun === 0) {
    return `${man}만원대`
  }
  return `${man}만${chun}천원대`
}

// "2026-08-16T09:20:00Z" -> "2026.08.16"
//
// 서버가 주는 글자는 끝에 Z 가 붙어 있다(UTC 라는 뜻). new Date 가
// 그것을 읽어 브라우저 시간대로 바꿔주므로, 여기서 9시간을 더할
// 필요가 없다 - 더하면 두 번 더해진다
export const formatDate = (text) => {
  if (!text) {
    return '-'
  }
  const d = new Date(text)
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

// "2026-08-16T09:20:00Z" -> "2026.08.16 18:20"
//
// 예시에서 09:20 이 18:20 이 되는 것이 시간대 변환이다.
// UTC 09:20 은 한국 시각으로 18:20 이다
export const formatDateTime = (text) => {
  if (!text) {
    return '-'
  }
  const d = new Date(text)
  return `${formatDate(text)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 날짜 -> "2026-08-17" (<input type="date"> 가 쓰는 모양)
//
// 위 formatDate 와 모양만 다르다(점 대신 하이픈). 사람이 읽을 자리와
// 입력칸에 넣을 자리가 달라서 둘을 나눠 두었다 - 입력칸은 이 모양만
// 받아들인다
export const toDateKey = (value) => {
  const d = new Date(value)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// 오늘로부터 며칠 전(-) / 뒤(+) 의 날짜
//
// 인기 레시피의 기간을 고를 때 쓴다 (shiftDays(-30) = 한 달 전).
//
// setDate 에 32 를 넣어도 다음 달로 알아서 넘어간다. 달의 마지막
// 날을 직접 따질 필요가 없다
export const shiftDays = (days) => {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d
}

// 25 -> "25분", 90 -> "1시간 30분"
//
// 120분을 "2시간 0분" 이 아니라 "2시간" 으로 적는다. 0분을 붙이면
// 군더더기가 된다
export const formatMinutes = (minutes) => {
  if (!minutes) {
    return '-'
  }
  if (minutes < 60) {
    return `${minutes}분`
  }

  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m === 0 ? `${h}시간` : `${h}시간 ${m}분`
}
