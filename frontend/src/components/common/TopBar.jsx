import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useCatalog } from '../../contexts/CatalogContext'
import { useLikes } from '../../contexts/LikesContext'
import { usePrefs } from '../../contexts/PrefsContext'
import { useProfile } from '../../contexts/ProfileContext'
import Icon, { LogoMark } from './Icon'
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
  const { searchSuggestions } = useCatalog()
  const { profile, notifications, unreadCount, isUnread, markAllRead } = useProfile()
  const navigate = useNavigate()

  /**
   * 패널을 열거나 닫는다.
   *
   * 이미 열려 있는 걸 또 누르면 닫히고(null), 다른 걸 누르면 그쪽이 열린다.
   * 상태가 하나라서 두 개가 동시에 열리는 일이 자동으로 막힌다.
   */
  function toggle(name) {
    setOpen((prev) => {
      const next = prev === name ? null : name

      // 알림을 여는 순간 "여기까지 확인했다"고 표시한다.
      // 하나씩 눌러야 읽음이 되면 배지가 계속 남아 있어서 잔소리처럼 느껴진다.
      if (next === 'bell') {
        markAllRead()
      }

      return next
    })
  }

  /** 열려 있는 패널을 닫는다. */
  function close() {
    setOpen(null)
  }

  // 알림을 꺼두면 숫자를 세지 않는다. 꺼놓고도 배지가 붙어 있으면 꺼진 것 같지 않다
  const unread = notify ? unreadCount : 0

  /**
   * 검색 화면으로 이동한다.
   *
   * encodeURIComponent 는 한글이나 공백을 주소에 넣을 수 있는 형태로 바꿔준다.
   * "김치 찌개" -> "%EA%B9%80%EC%B9%98%20%EC%B0%8C%EA%B0%9C"
   */
  function search(text) {
    const trimmed = text.trim()
    if (!trimmed) return // 빈 검색어로는 이동하지 않는다

    close() // 열려 있던 패널을 닫고 넘어간다
    navigate(`/search?q=${encodeURIComponent(trimmed)}`)
  }

  /** 검색창에서 엔터를 치거나 돋보기를 누르면 실행된다. */
  function onSubmit(e) {
    e.preventDefault() // 폼 기본 동작(새로고침)을 막는다
    search(keyword)
  }

  /** 로그아웃하고 홈으로 보낸다. */
  function onLogout() {
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
              {searchSuggestions.map((text) => (
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
                notifications.length === 0 ? (
                  <p className="topbar__panel-foot muted">
                    아직 알림이 없어요. 내 글에 댓글이나 좋아요가 달리면 여기에
                    모입니다.
                  </p>
                ) : (
                  <ul className="topbar__notis">
                    {notifications.map((noti) => (
                      <li key={noti.id} className={isUnread(noti) ? 'is-unread' : ''}>
                        <span className="topbar__noti-icon" aria-hidden="true">
                          {noti.icon}
                        </span>
                        <Link
                          to={noti.link}
                          className="topbar__noti-body"
                          onClick={close}
                        >
                          <strong>{noti.title}</strong>
                          <span className="muted">{noti.body}</span>
                          <span className="topbar__noti-date muted">
                            {formatDate(noti.created_at)}
                          </span>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )
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
                {/* 레벨은 서버에서 계산해 온다. 아직 못 받았으면 줄을 통째로 뺀다.
                    'Lv.- ' 같은 자리표시자를 보여주면 고장난 것처럼 보인다 */}
                {profile && (
                  <span className="topbar__level">
                    Lv.{profile.level} {profile.title}
                  </span>
                )}
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
