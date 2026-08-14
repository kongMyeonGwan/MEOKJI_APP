import { Link, NavLink } from 'react-router-dom'
import './Header.css'

/**
 * 상단 네비게이션.
 * STEP 6에서 AuthContext를 붙여 로그인 상태에 따라 메뉴를 전환한다.
 * 지금은 Guest 상태 메뉴만 표시한다.
 */
export default function Header() {
  const isAuthenticated = false // STEP 6에서 useAuth()로 교체

  return (
    <header className="header">
      <div className="container header__inner">
        <Link to="/" className="header__logo">
          뭐먹지?
        </Link>

        <nav className="header__nav">
          {isAuthenticated ? (
            <>
              <NavLink to="/history">History</NavLink>
              <NavLink to="/community">커뮤니티</NavLink>
              <NavLink to="/mypage">MyPage</NavLink>
              <button type="button" className="header__link-btn">로그아웃</button>
            </>
          ) : (
            <>
              <NavLink to="/community">커뮤니티</NavLink>
              <NavLink to="/login">로그인</NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
