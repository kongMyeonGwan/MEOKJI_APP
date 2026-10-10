import { Link, useLocation } from 'react-router-dom'

// 없는 주소로 들어왔을 때 보여주는 화면.
//
// App.jsx 의 <Route path="*"> 가 여기로 보낸다. 주소를 잘못 친 경우도
// 있지만, 아직 안 연 화면으로 가는 링크를 눌렀을 때도 여기로 온다
// (상단바 프로필 메뉴의 "AI 추천 기록" 이 그렇다).
//
// MainLayout 안에 들어 있어서 사이드바와 상단바는 그대로 보인다.
// 길을 잃어도 메뉴로 빠져나갈 수 있어야 한다.
export default function NotFoundPage() {
  const location = useLocation() // 지금 주소 정보. location.pathname = '/어떤/주소'

  return (
    <div className="card">
      <h2 style={{ marginTop: 0 }}>404</h2>
      {/* 어느 주소로 들어왔는지 같이 보여준다. 오타를 낸 경우
          그 자리에서 바로 알아챌 수 있다 */}
      <p className="muted">요청하신 페이지를 찾을 수 없습니다: {location.pathname}</p>
      {/* 돌아갈 길을 하나 둔다. 404 만 띄우고 끝내면 뒤로가기
          말고는 방법이 없다.

          style 에 CSS 변수를 쓴다. 색을 글자로 박으면 다크 모드에서
          안 바뀐다 - var(--color-accent) 는 테마에 따라 값이 달라진다 */}
      <Link to="/" style={{ color: 'var(--color-accent)' }}>
        홈으로 돌아가기
      </Link>
    </div>
  )
}
