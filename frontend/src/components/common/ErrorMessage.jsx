/** 기능별 에러 표시. 한 기능이 실패해도 다른 기능은 살아있어야 한다(§67). */
export default function ErrorMessage({ message }) {
  if (!message) return null
  return (
    <p style={{ color: 'var(--color-error)', margin: '4px 0' }}>{message}</p>
  )
}
