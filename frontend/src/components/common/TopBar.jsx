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

// 상단 바. 메뉴 버튼 + AI 검색 도우미 + 알림 / 찜 / 프로필.
//
// 검색창은 홈 화면 가운데(FoodFinder)에 있다.
// 여기에도 두면 같은 입력칸이 한 화면에 두 개가 된다.
//
// 열리는 패널이 셋(AI 도우미, 알림, 프로필)인데 상태는 하나만 쓴다.
// 따로 두면 두 개가 동시에 열리는 걸 매번 막아줘야 한다.
export default function TopBar({ onToggleNav, navOpen }) {
  // 지금 열려 있는 패널 이름. 'ai' 나 'bell' 이나 'profile', 아무것도 안 열렸으면 null
  const [open, setOpen] = useState(null)

  const { isAuthenticated, user, logout } = useAuth()
  const { count: likeCount } = useLikes()
  const { notify } = usePrefs()
  const { searchSuggestions } = useCatalog()
  const { notifications, unreadCount, isUnread, markAllRead } = useProfile()
  const navigate = useNavigate()

  // 패널을 열거나 닫는다.
  // 열려 있는 걸 또 누르면 닫히고 다른 걸 누르면 그쪽이 열린다.
  function toggle(name) {
    setOpen(function (prev) {
      let next = name
      if (prev === name) {
        next = null
      }

      // 알림을 여는 순간 여기까지 확인했다고 표시한다.
      // 하나씩 눌러야 읽음이 되면 배지가 계속 남아 있어서 거슬린다.
      if (next === 'bell') {
        markAllRead()
      }

      return next
    })
  }

  // 열려 있는 패널을 닫는다
  function close() {
    setOpen(null)
  }

  // 알림을 꺼놨으면 숫자를 안 센다. 꺼놨는데 배지가 붙어 있으면 이상하다.
  let unread = 0
  if (notify) {
    unread = unreadCount
  }

  // AI 도우미에서 예시를 누르면 검색 화면으로 보낸다.
  // encodeURIComponent 는 한글이나 공백을 주소에 넣을 수 있게 바꿔준다.
  function search(text) {
    const trimmed = text.trim()
    if (!trimmed) {
      // 빈 검색어로는 이동 안 한다
      return
    }

    close()
    navigate('/search?q=' + encodeURIComponent(trimmed))
  }

  // 로그아웃하고 홈으로 보낸다
  function onLogout() {
    close()
    logout()
    navigate('/')
  }

  // AI 도우미 버튼 색
  let aiButtonClass = 'topbar__ai-btn'
  if (open === 'ai') {
    aiButtonClass = 'topbar__ai-btn is-open'
  }

  return (
    <header className="topbar">
      <button
        type="button"
        className="topbar__menu-btn"
        onClick={onToggleNav}
        aria-label="메뉴"
      >
        <Icon name="menu" size={22} />
      </button>

      {/* 모바일에서만 보이는 로고 (사이드바가 접혀 있어서) */}
      <Link to="/" className="topbar__logo">
        <LogoMark size={26} />
        <span className="topbar__logo-text">오늘 뭐 먹지?</span>
      </Link>

      <div className="topbar__ai">
        <button
          type="button"
          className={aiButtonClass}
          onClick={() => toggle('ai')}
        >
          <span aria-hidden="true">✨</span>
          <span className="topbar__ai-label">AI 검색 도우미</span>
        </button>

        {open === 'ai' && (
          <div className="topbar__panel topbar__panel--ai">
            <p className="topbar__panel-title">이렇게 물어보셔도 돼요</p>
            <ul className="topbar__ai-list">
              {searchSuggestions.map(function (text) {
                return (
                  <li key={text}>
                    <button type="button" onClick={() => search(text)}>
                      {text}
                    </button>
                  </li>
                )
              })}
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
                    {notifications.map(function (noti) {
                      let itemClass = ''
                      if (isUnread(noti)) {
                        itemClass = 'is-unread'
                      }

                      return (
                        <li key={noti.id} className={itemClass}>
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
                      )
                    })}
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
            >
              <span className="topbar__avatar" aria-hidden="true">
                {user.nickname.slice(0, 1)}
              </span>
              <span className="topbar__who">
                <span className="topbar__name">안녕하세요, {user.nickname}님!</span>
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

      {/* 바깥을 눌러서 닫으려고 까는 투명 레이어. 패널이 열렸을 때만 있다 */}
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
