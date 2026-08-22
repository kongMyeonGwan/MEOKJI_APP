import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Icon, { LogoMark } from './Icon'
import '../../styles/components/common/TopBar.css'

/**
 * 상단 바 — 검색 + 알림 + 프로필.
 *
 * 웹 모드 : 검색창이 넓게 펼쳐지고 우측에 알림·프로필
 * 앱 모드 : 좌측에 로고가 추가되고 검색창은 그 아래 줄로 내려간다
 */
export default function TopBar() {
  const [keyword, setKeyword] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)

  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()

  const onSearch = (e) => {
    e.preventDefault()
    const trimmed = keyword.trim()
    if (!trimmed) return
    navigate(`/recipe/${encodeURIComponent(trimmed)}`)
    setKeyword('')
  }

  const onLogout = () => {
    setMenuOpen(false)
    logout()
    navigate('/')
  }

  return (
    <header className="topbar">
      {/* 앱 모드에서만 보이는 로고 (사이드바가 없으므로) */}
      <Link to="/" className="topbar__logo">
        <LogoMark size={24} />
        뭐먹지?
      </Link>

      <form className="topbar__search" onSubmit={onSearch} role="search">
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="검색어를 입력하세요 (예: 김치찌개)"
          aria-label="음식 검색"
        />
        <button type="submit" aria-label="검색">
          <Icon name="search" size={19} />
        </button>
      </form>

      <div className="topbar__actions">
        <button type="button" className="topbar__bell" aria-label="알림">
          <Icon name="bell" size={21} />
          {/* 읽지 않은 알림 표시. 알림 기능은 아직 없어 항상 켜둔다 */}
          <span className="topbar__dot" aria-hidden="true" />
        </button>

        {isAuthenticated ? (
          <div className="topbar__profile">
            <button
              type="button"
              className="topbar__profile-btn"
              onClick={() => setMenuOpen((prev) => !prev)}
              aria-expanded={menuOpen}
            >
              <span className="topbar__avatar" aria-hidden="true">
                {user.nickname.slice(0, 1)}
              </span>
              <span className="topbar__name">{user.nickname}님</span>
              <span className="topbar__caret" aria-hidden="true">
                ⌄
              </span>
            </button>

            {menuOpen && (
              <>
                {/* 바깥을 눌러 닫기 위한 투명 레이어 */}
                <button
                  type="button"
                  className="topbar__backdrop"
                  aria-label="메뉴 닫기"
                  onClick={() => setMenuOpen(false)}
                />
                <div className="topbar__menu">
                  <Link to="/mypage" onClick={() => setMenuOpen(false)}>
                    MyPage
                  </Link>
                  <Link to="/history" onClick={() => setMenuOpen(false)}>
                    History
                  </Link>
                  <button type="button" onClick={onLogout}>
                    로그아웃
                  </button>
                </div>
              </>
            )}
          </div>
        ) : (
          <Link to="/login" className="btn btn--sm">
            로그인
          </Link>
        )}
      </div>
    </header>
  )
}
