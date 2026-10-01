import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Icon, { LogoMark } from '../../components/common/Icon'
import { getCatalog } from '../../api/catalog'
import { getLikes } from '../../api/likes'
import { getNotifications } from '../../api/users'
import { formatDate } from '../../utils/format'
import './TopBar.css'

// 상단 바. 메뉴 버튼 + AI 검색 도우미 + 알림 / 찜 / 프로필.
//
// 알림 "읽음" 은 서버에 저장하지 않는다.
// 알림 창을 마지막으로 연 시각(readAt)을 브라우저에 저장해두고
// 그보다 나중에 온 알림을 "안 읽음" 으로 센다.

const READ_AT_KEY = 'meokji.notify.readAt'

export default function TopBar({ onToggleNav, user, onLogout, notify }) {
  // 지금 열려 있는 패널: 'ai' / 'bell' / 'profile' / null(닫힘)
  const [open, setOpen] = useState(null)

  const [searchSuggestions, setSearchSuggestions] = useState([])
  const [likeCount, setLikeCount] = useState(0)
  const [notifications, setNotifications] = useState([])
  const [readAt, setReadAt] = useState(localStorage.getItem(READ_AT_KEY) || '')

  const navigate = useNavigate()
  const location = useLocation()

  // AI 도우미에 보여줄 예시 검색어. 처음 한 번만 받아온다.
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setSearchSuggestions(data.search_suggestions)
      } catch (err) {
        // 못 받아와도 예시만 안 보일 뿐이다
      }
    }
    load()
  }, [])

  // 찜 개수와 알림. 로그인했을 때만, 화면을 옮길 때마다 다시 받아온다.
  useEffect(() => {
    if (!user) {
      setLikeCount(0)
      setNotifications([])
      return
    }

    const load = async () => {
      try {
        const likes = await getLikes()
        setLikeCount(likes.count)

        const notis = await getNotifications()
        setNotifications(notis)
      } catch (err) {
        // 못 받아와도 숫자만 안 보일 뿐이다
      }
    }
    load()
  }, [user, location.pathname])

  // 마지막으로 알림 창을 연 뒤에 온 알림인지
  const isUnread = (noti) => {
    return noti.created_at > readAt
  }

  // 안 읽은 알림 개수. 알림을 꺼뒀으면 0
  const unreadCount = notify ? notifications.filter(isUnread).length : 0

  // 패널 열기/닫기. 열린 걸 또 누르면 닫힌다.
  const togglePanel = (name) => {
    if (open === name) {
      setOpen(null)
      return
    }

    setOpen(name)

    // 알림 창을 열면 지금 시각을 "여기까지 읽음" 으로 저장한다
    if (name === 'bell') {
      const now = new Date().toISOString()
      localStorage.setItem(READ_AT_KEY, now)
      setReadAt(now)
    }
  }

  const closePanel = () => {
    setOpen(null)
  }

  // AI 도우미의 예시를 누르면 검색 화면으로 보낸다
  const search = (text) => {
    closePanel()
    navigate(`/search?q=${encodeURIComponent(text)}`)
  }

  const handleLogout = () => {
    closePanel()
    onLogout()
    navigate('/')
  }

  return (
    <header className="topbar">
      <button type="button" className="topbar__menu-btn" onClick={onToggleNav}>
        <Icon name="menu" size={22} />
      </button>

      {/* 모바일에서만 보이는 로고 */}
      <Link to="/" className="topbar__logo">
        <LogoMark size={26} />
        <span className="topbar__logo-text">오늘 뭐 먹지?</span>
      </Link>

      {/* AI 검색 도우미 */}
      <div className="topbar__ai">
        <button
          type="button"
          className={open === 'ai' ? 'topbar__ai-btn is-open' : 'topbar__ai-btn'}
          onClick={() => togglePanel('ai')}
        >
          <span>✨</span>
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
        {/* 알림 */}
        <div className="topbar__bell-wrap">
          <button type="button" className="topbar__icon-btn" onClick={() => togglePanel('bell')}>
            <Icon name="bell" size={21} />
            {unreadCount > 0 && <span className="badge topbar__count">{unreadCount}</span>}
          </button>

          {open === 'bell' && (
            <div className="topbar__panel topbar__panel--bell">
              <p className="topbar__panel-title">알림</p>

              {!notify && (
                <p className="topbar__panel-foot muted">
                  알림을 꺼두셨어요. <Link to="/settings">설정</Link>에서 다시 켤 수 있어요.
                </p>
              )}

              {notify && notifications.length === 0 && (
                <p className="topbar__panel-foot muted">
                  아직 알림이 없어요. 내 글에 댓글이나 좋아요가 달리면 여기에 모입니다.
                </p>
              )}

              {notify && notifications.length > 0 && (
                <ul className="topbar__notis">
                  {notifications.map((noti) => (
                    <li key={noti.id} className={isUnread(noti) ? 'is-unread' : ''}>
                      <span className="topbar__noti-icon">{noti.icon}</span>
                      <Link to={noti.link} className="topbar__noti-body" onClick={closePanel}>
                        <strong>{noti.title}</strong>
                        <span className="muted">{noti.body}</span>
                        <span className="topbar__noti-date muted">{formatDate(noti.created_at)}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

        {/* 찜 */}
        <Link to="/liked" className="topbar__icon-btn">
          <Icon name="heart" size={21} />
          {likeCount > 0 && <span className="badge topbar__count">{likeCount}</span>}
        </Link>

        {/* 프로필 (로그인 안 했으면 로그인 버튼) */}
        {user ? (
          <div className="topbar__profile">
            <button type="button" className="topbar__profile-btn" onClick={() => togglePanel('profile')}>
              <span className="topbar__avatar">{user.nickname.slice(0, 1)}</span>
              <span className="topbar__who">
                <span className="topbar__name">안녕하세요, {user.nickname}님!</span>
              </span>
              <span className="topbar__caret">⌄</span>
            </button>

            {open === 'profile' && (
              <div className="topbar__panel topbar__menu">
                <Link to="/mypage" onClick={closePanel}>MyPage</Link>
                <Link to="/history" onClick={closePanel}>History</Link>
                <Link to="/ai-history" onClick={closePanel}>AI 추천 기록</Link>
                <Link to="/settings" onClick={closePanel}>설정</Link>
                <button type="button" onClick={handleLogout}>로그아웃</button>
              </div>
            )}
          </div>
        ) : (
          <Link to="/login" className="btn btn--sm">
            로그인
          </Link>
        )}
      </div>

      {/* 패널 바깥을 누르면 닫히게 까는 투명한 판 */}
      {open && <button type="button" className="topbar__backdrop" onClick={closePanel} />}
    </header>
  )
}
