// 에러 문구를 빨간 글씨로 보여주는 컴포넌트.
// 한 기능이 실패해도 다른 기능은 살아있어야 해서 화면마다 이걸 쓴다.
export default function ErrorMessage({ message }) {
  if (!message) {
    return null
  }

  return (
    <p style={{ color: 'var(--color-error)', margin: '4px 0' }}>{message}</p>
  )
}
