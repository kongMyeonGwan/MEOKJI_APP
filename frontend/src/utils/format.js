/**
 * 화면 표시용 포맷 함수.
 *
 * 서버는 시각을 항상 UTC로 내려준다(끝의 Z). 화면에서는 한국 시각으로 보여준다.
 * new Date()가 브라우저 시간대로 자동 변환해주므로 별도 계산이 필요 없다.
 */

/** 8500 -> "8,500원" */
export function formatPrice(value) {
  if (value === null || value === undefined) return '-'
  return `${value.toLocaleString('ko-KR')}원`
}

/** "2026-08-16T09:20:00Z" -> "2026.08.16" */
export function formatDate(isoString) {
  if (!isoString) return '-'
  const d = new Date(isoString)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

/** "2026-08-16T09:20:00Z" -> "2026.08.16 18:20" */
export function formatDateTime(isoString) {
  if (!isoString) return '-'
  const d = new Date(isoString)
  const pad = (n) => String(n).padStart(2, '0')
  return `${formatDate(isoString)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * Date 또는 ISO 문자열 -> "2026-08-17"
 *
 * <input type="date">가 요구하는 형식이자, 기간 비교의 기준 형식이다.
 * YYYY-MM-DD 는 문자열끼리 사전순 비교해도 날짜순과 일치해서
 * start <= key <= end 로 범위 판정을 할 수 있다.
 *
 * toISOString()을 쓰면 안 된다. UTC로 변환되기 때문에
 * 한국 시각 새벽 0~9시가 전날로 밀린다.
 */
export function toDateKey(value) {
  const d = value instanceof Date ? value : new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 오늘로부터 n일 전(음수) / 후(양수)의 Date */
export function shiftDays(days, from = new Date()) {
  const d = new Date(from)
  d.setDate(d.getDate() + days)
  return d
}

/** 25 -> "25분" */
export function formatMinutes(minutes) {
  if (!minutes) return '-'
  if (minutes < 60) return `${minutes}분`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m === 0 ? `${h}시간` : `${h}시간 ${m}분`
}
