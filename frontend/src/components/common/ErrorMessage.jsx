// 에러 문구를 빨간 글씨로 보여준다. message 가 비어 있으면 아무것도 안 그린다.
export default function ErrorMessage({ message }) {
  if (!message) {
    return null
  }

  return <p style={{ color: 'var(--color-error)', margin: '4px 0' }}>{message}</p>
}
