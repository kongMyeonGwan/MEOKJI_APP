import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useLikes } from '../../contexts/LikesContext'
import { usePrefs } from '../../contexts/PrefsContext'
import Icon, { LogoMark } from './Icon'
import { MOCK_NOTIFICATIONS, MOCK_PROFILE, MOCK_SEARCH_SUGGESTIONS } from '../../mocks'
import { formatDate } from '../../utils/format'
import '../../styles/components/common/TopBar.css'

/**
 * 상단 바 — 메뉴 토글 + 검색 + 알림 · 찜 · 프로필.
 *
 * 검색창을 여기 둔다. 어느 화면에 있든 "다른 걸 찾고 싶다"가 한 번에 되어야 해서,
 * 화면마다 따로 두는 대신 항상 같은 자리에 있는 상단바가 맞는 자리다.
 * 대시보드 안의 검색 카드(FoodFinder)는 추천 예시를 함께 보여주는 역할이라
 * 겹쳐 보여도 하는 일이 다르다.
 *
 * 열리는 패널(AI 도우미 · 알림 · 프로필)이 셋인데 상태를 하나로 둔다.
 * 따로 두면 두 개가 동시에 열려 서로 겹치는 상태를 매번 막아줘야 한다.
 */
export default function TopBar({ onToggleNav, navOpen }) {
  const [open, setOpen] = useState(null) // 'ai' | 'bell' | 'profile' | null
  const [keyword, setKeyword] = useState('')

  const { isAuthenticated, user, logout } = useAuth()
  const { count: likeCount } = useLikes()
  const { notify } = usePrefs()
  const navigate = useNavigate()

  const toggle = (name) => setOpen((prev) => (prev === name ? null : name))
  const close = () => setOpen(null)

  // 알림을 꺼두면 숫자를 세지 않는다. 꺼놓고도 배지가 붙어 있으면 꺼진 것 같지 않다
  const unread = notify ? MOCK_NOTIFICATIONS.filter((n) => !n.read).length : 0

  const search = (text) => {
    const trimmed = text.trim()
    if (!trimmed) return
    close()
    navigate(`/search?q=${encodeURIComponent(trimmed)}`)
  }

  const onSubmit = (e) => {
    e.preventDefault()
    search(keyword)
  }

  const onLogout = () => {
    close()
    logout()
    navigate('/')
  }

  return (
    <header className="topbar">
      <button
        type="button"
        className="topbar__menu-btn"
        onClick={onToggleNav}
        aria-label="메뉴"
        aria-expanded={navOpen}
      >
        <Icon name="menu" size={22} />
      </button>

      {/* 앱 모드에서만 보이는 로고 (사이드바가 접혀 있으므로) */}
      <Link to="/" className="topbar__logo">
        <LogoMark size={26} />
        <span className="topbar__logo-text">오늘 뭐 먹지?</span>
      </Link>

      <form className="topbar__search" onSubmit={onSubmit} role="search">
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="오늘 뭐 드시고 싶으세요? 메뉴나 재료를 입력해보세요"
          maxLength={150}
          aria-label="메뉴 · 재료 검색"
        />
        <button type="submit" className="topbar__search-btn" aria-label="검색">
          <Icon name="search" size={20} />
        </button>
      </form>

      <div className="topbar__ai">
        <button
          type="button"
          className={`topbar__ai-btn${open === 'ai' ? ' is-open' : ''}`}
          onClick={() => toggle('ai')}
          aria-expanded={open === 'ai'}
        >
          <span aria-hidden="true">✨</span>
          <span className="topbar__ai-label">AI 검색 도우미</span>
        </button>

        {open === 'ai' && (
          <div className="topbar__panel topbar__panel--ai">
            <p className="topbar__panel-title">이렇게 물어보셔도 돼요</p>
            <ul className="topbar__ai-list">
              {MOCK_SEARCH_SUGGESTIONS.map((text) => (
                <li key={text}>
                  <button type="button" onClick={() => search(text)}>
                    {text}
                  </button>
                </li>
              ))}
            </ul>
            <p className="topbar__panel-foot muted">
              메뉴 이름뿐 아니라 재료 · 예산 · 상황으로도 찾을 수 있어요.
            </p>
          </div>
        )}
      </div>

      <div className="topbar__actions">
        <div className="topbar__bell-wrap">
          <button
            type="button"
            className="topbar__icon-btn"
            onClick={() => toggle('bell')}
            aria-expanded={open === 'bell'}
            aria-label="알림"
          >
            <Icon name="bell" size={21} />
            {unread > 0 && <span className="badge topbar__count">{unread}</span>}
          </button>

          {open === 'bell' && (
            <div className="topbar__panel topbar__panel--bell">
              <p className="topbar__panel-title">알림</p>

              {!notify ? (
                <p className="topbar__panel-foot muted">
                  알림을 꺼두셨어요. <Link to="/settings">설정</Link>에서 다시 켤 수
                  있어요.
                </p>
              ) : (
                <ul className="topbar__notis">
                  {MOCK_NOTIFICATIONS.map((noti) => (
                    <li key={noti.id} className={noti.read ? '' : 'is-unread'}>
                      <span className="topbar__noti-icon" aria-hidden="true">
                        {noti.icon}
                      </span>
                      <span className="topbar__noti-body">
                        <strong>{noti.title}</strong>
                        <span className="muted">{noti.body}</span>
                        <span className="topbar__noti-date muted">
                          {formatDate(noti.created_at)}
                        </span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

        <Link to="/liked" className="topbar__icon-btn" aria-label="찜한 레시피">
          <Icon name="heart" size={21} />
          {likeCount > 0 && <span className="badge topbar__count">{likeCount}</span>}
        </Link>

        {isAuthenticated ? (
          <div className="topbar__profile">
            <button
              type="button"
              className="topbar__profile-btn"
              onClick={() => toggle('profile')}
              aria-expanded={open === 'profile'}
            >
              <span className="topbar__avatar" aria-hidden="true">
                {user.nickname.slice(0, 1)}
              </span>
              <span className="topbar__who">
                <span className="topbar__name">안녕하세요, {user.nickname}님!</span>
                <span className="topbar__level">
                  Lv.{MOCK_PROFILE.level} {MOCK_PROFILE.title}
                </span>
              </span>
              <span className="topbar__caret" aria-hidden="true">
                ⌄
              </span>
            </button>

            {open === 'profile' && (
              <div className="topbar__panel topbar__menu">
                <Link to="/mypage" onClick={close}>
                  MyPage
                </Link>
                <Link to="/history" onClick={close}>
                  History
                </Link>
                <Link to="/ai-history" onClick={close}>
                  AI 추천 기록
                </Link>
                <Link to="/settings" onClick={close}>
                  설정
                </Link>
                <button type="button" onClick={onLogout}>
                  로그아웃
                </button>
              </div>
            )}
          </div>
        ) : (
          <Link to="/login" className="btn btn--sm">
            로그인
          </Link>
        )}
      </div>

      {/* 바깥을 눌러 닫기 위한 투명 레이어. 열린 패널이 있을 때만 깐다 */}
      {open && (
        <button
          type="button"
          className="topbar__backdrop"
          aria-label="닫기"
          onClick={close}
        />
      )}
    </header>
  )
}
