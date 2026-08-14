import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="card">
      <h2 style={{ marginTop: 0 }}>404</h2>
      <p className="muted">요청하신 페이지를 찾을 수 없습니다.</p>
      <Link to="/" style={{ color: 'var(--color-accent)' }}>홈으로 돌아가기</Link>
    </div>
  )
}
