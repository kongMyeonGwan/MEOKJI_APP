// 화면에 보여줄 때 쓰는 포맷 함수들

// 한 자리 숫자 앞에 0 붙이기. 8 -> "08"
const pad = (n) => {
  return String(n).padStart(2, '0')
}

// 8500 -> "8,500원"
export const formatPrice = (value) => {
  if (value === null || value === undefined) {
    return '-'
  }
  return `${value.toLocaleString()}원`
}

// 7200 -> "7천원대", 15400 -> "1만5천원대"
export const priceBand = (value) => {
  if (!value) {
    return null
  }

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
export const formatDate = (text) => {
  if (!text) {
    return '-'
  }
  const d = new Date(text)
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

// "2026-08-16T09:20:00Z" -> "2026.08.16 18:20"
export const formatDateTime = (text) => {
  if (!text) {
    return '-'
  }
  const d = new Date(text)
  return `${formatDate(text)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 날짜 -> "2026-08-17" (<input type="date"> 가 쓰는 모양)
export const toDateKey = (value) => {
  const d = new Date(value)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// 오늘로부터 며칠 전(-) / 뒤(+) 의 날짜
export const shiftDays = (days) => {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d
}

// 25 -> "25분", 90 -> "1시간 30분"
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
