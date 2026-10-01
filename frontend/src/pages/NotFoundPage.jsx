import { Link, useLocation } from 'react-router-dom'

// 없는 주소로 들어왔을 때 보여주는 화면
export default function NotFoundPage() {
  const location = useLocation() // 지금 주소 정보. location.pathname = '/어떤/주소'

  return (
    <div className="card">
      <h2 style={{ marginTop: 0 }}>404</h2>
      <p className="muted">요청하신 페이지를 찾을 수 없습니다: {location.pathname}</p>
      <Link to="/" style={{ color: 'var(--color-accent)' }}>
        홈으로 돌아가기
      </Link>
    </div>
  )
}
