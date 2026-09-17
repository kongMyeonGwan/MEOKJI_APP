// 화면에 보여줄 때 쓰는 포맷 함수들.
// 서버는 시각을 UTC(끝에 Z)로 주는데 new Date() 가 알아서 한국 시각으로 바꿔준다.

// 한 자리 숫자 앞에 0 붙이기. 8 -> "08"
function pad(n) {
  return String(n).padStart(2, '0')
}

// 8500 -> "8,500원"
export function formatPrice(value) {
  if (value === null || value === undefined) {
    return '-'
  }
  return value.toLocaleString('ko-KR') + '원'
}

// 7200 -> "7천원대", 15400 -> "1만5천원대"
export function priceBand(value) {
  if (!value) {
    return null
  }

  const man = Math.floor(value / 10000)
  const chun = Math.floor((value % 10000) / 1000)

  if (man === 0) {
    return chun + '천원대'
  }
  if (chun === 0) {
    return man + '만원대'
  }
  return man + '만' + chun + '천원대'
}

// "2026-08-16T09:20:00Z" -> "2026.08.16"
export function formatDate(isoString) {
  if (!isoString) {
    return '-'
  }

  const d = new Date(isoString)
  return d.getFullYear() + '.' + pad(d.getMonth() + 1) + '.' + pad(d.getDate())
}

// "2026-08-16T09:20:00Z" -> "2026.08.16 18:20"
export function formatDateTime(isoString) {
  if (!isoString) {
    return '-'
  }

  const d = new Date(isoString)
  return formatDate(isoString) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

// Date 또는 문자열 -> "2026-08-17"
// <input type="date"> 가 이 모양을 요구한다.
// toISOString() 은 UTC 라서 새벽에 날짜가 하루 밀린다. 그래서 직접 만든다.
export function toDateKey(value) {
  let d = value
  if (!(value instanceof Date)) {
    d = new Date(value)
  }
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

// 오늘로부터 며칠 전(-) / 뒤(+) 의 날짜
export function shiftDays(days, from) {
  let base = from
  if (!base) {
    base = new Date()
  }

  const d = new Date(base)
  d.setDate(d.getDate() + days)
  return d
}

// 25 -> "25분", 90 -> "1시간 30분"
export function formatMinutes(minutes) {
  if (!minutes) {
    return '-'
  }
  if (minutes < 60) {
    return minutes + '분'
  }

  const h = Math.floor(minutes / 60)
  const m = minutes % 60

  if (m === 0) {
    return h + '시간'
  }
  return h + '시간 ' + m + '분'
}
