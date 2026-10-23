import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import Icon, { LogoMark } from '../../components/common/Icon'

import { getCatalog } from '../../api/catalog'
import { getFavorites } from '../../api/favorites'
import { getNotifications } from '../../api/users'

import { formatDate } from '../../utils/format'

// 알림 "읽음" 은 서버에 저장하지 않는다.
// 알림 창을 마지막으로 연 시각(readAt)을 브라우저에 저장해두고
// 그보다 나중에 온 알림을 "안 읽음" 으로 센다.
const READ_AT_KEY = 'meokji.notify.readAt'

// 알림은 화면에서 숨겨둔다. 알림 표가 없어서 하나씩 읽음 처리를 할 수 없다.
// 서버 API(/api/users/me/notifications)는 그대로 있으니 true 로 바꾸면 다시 보인다.
const SHOW_NOTIFICATIONS = false

export default function TopBar({ onToggleNav, user, onLogout, notify, likeCount, onLikeCountChange }) {
  // ---------------------------------------
  // state
  // ---------------------------------------

  // 현재 열려있는 메뉴
  // null      : 아무것도 안 열림
  // 'ai'      : AI 검색
  // 'bell'    : 알림
  // 'profile' : 프로필
  const [open, setOpen] = useState(null)

  // AI 검색 추천 문장
  const [searchSuggestions, setSearchSuggestions] = useState([])

  // 알림 목록
  const [notifications, setNotifications] = useState([])

  // 알림 창을 마지막으로 연 시각
  const [readAt, setReadAt] = useState(localStorage.getItem(READ_AT_KEY) || '')

  // 성별 선택
  const profileImage =
    user?.gender === 'MALE'
      ? '/images/myface/avatar_male.png'
      : user?.gender === 'FEMALE'
        ? '/images/myface/avatar_female.png'
        : '/images/myface/avatar_default.png'

  const navigate = useNavigate()
  const location = useLocation()

  // ---------------------------------------
  // 처음 화면이 실행될 때
  // AI 검색 추천 문장을 서버에서 가져온다.
  // ---------------------------------------

  useEffect(() => {
    const loadCatalog = async () => {
      try {
        const data = await getCatalog()

        setSearchSuggestions(data.searchSuggestions || [])
      } catch (error) {
        console.log('검색 추천 목록을 가져오지 못했습니다.')
      }
    }

    loadCatalog()
  }, [])

  // ---------------------------------------
  // 로그인하면 찜 목록과 알림을 가져온다.
  // 화면을 옮길 때마다 다시 받아온다 - 찜을 누르고 돌아오면
  // 상단바 숫자도 같이 맞아야 한다.
  // ---------------------------------------

  useEffect(() => {
    // 로그인하지 않았으면 서버 요청 안 함
    if (!user) {
      onLikeCountChange(0)
      setNotifications([])

      return
    }

    const loadUserData = async () => {
      try {
        // 찜 목록
        const likes = await getFavorites()

        onLikeCountChange(likes.count)

        // 알림 목록. 숨겨둔 동안은 서버에 묻지 않는다
        if (SHOW_NOTIFICATIONS) {
          const notificationList = await getNotifications()

          setNotifications(notificationList)
        }
      } catch (error) {
        console.log('사용자 정보를 가져오지 못했습니다.')
      }
    }

    loadUserData()
  }, [user, location.pathname])

  // ---------------------------------------
  // 안 읽은 알림
  // ---------------------------------------

  // 마지막으로 알림 창을 연 뒤에 온 알림인지
  const isUnread = (noti) => {
    return noti.createdAt > readAt
  }

  // 안 읽은 알림 개수. 알림을 꺼뒀으면 0
  const unreadCount = notify ? notifications.filter(isUnread).length : 0

  // ---------------------------------------
  // 메뉴 열기 / 닫기
  // ---------------------------------------

  const togglePanel = (name) => {
    // 이미 열려있는 메뉴를 다시 누르면 닫기
    if (open === name) {
      setOpen(null)
      return
    }

    // 다른 메뉴 열기
    setOpen(name)

    // 알림 창을 열면 지금 시각을 "여기까지 읽음" 으로 저장한다
    if (name === 'bell') {
      const now = new Date().toISOString()

      localStorage.setItem(READ_AT_KEY, now)
      setReadAt(now)
    }
  }

  // 모든 메뉴 닫기
  const closePanel = () => {
    setOpen(null)
  }

  // ---------------------------------------
  // AI 추천 검색
  // ---------------------------------------

  const search = (text) => {
    // AI 메뉴 닫기
    closePanel()

    // 검색 페이지로 이동
    navigate('/search?q=' + encodeURIComponent(text))
  }

  // ---------------------------------------
  // 로그아웃
  // ---------------------------------------

  const handleLogout = () => {
    // 프로필 메뉴 닫기
    closePanel()

    // App.jsx의 로그아웃 함수 실행
    onLogout()

    // 메인 페이지 이동
    navigate('/')
  }

  return (
    <header className="topbar">
      {/* =========================
          사이드바 메뉴 버튼
         ========================= */}

      <button type="button" className="topbar__menu-btn" onClick={onToggleNav}>
        <Icon name="menu" size={22} />
      </button>

      {/* =========================
          로고
         ========================= */}

      <Link to="/" className="topbar__logo">
        <LogoMark size={26} />

        <span className="topbar__logo-text">오늘 뭐 먹지?</span>
      </Link>

      {/* =========================
          AI 검색
         ========================= */}

      <div className="topbar__ai">
        <button
          type="button"
          className={open === 'ai' ? 'topbar__ai-btn is-open' : 'topbar__ai-btn'}
          onClick={() => togglePanel('ai')}
        >
          <span>✨</span>

          <span className="topbar__ai-label">AI 검색 도우미</span>
        </button>

        {/* AI 메뉴가 열렸을 때만 보여준다 */}

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

            <p className="topbar__panel-foot muted">메뉴 이름뿐 아니라 재료 · 예산 · 상황으로도 찾을 수 있어요.</p>
          </div>
        )}
      </div>

      {/* =========================
          오른쪽 메뉴
         ========================= */}

      <div className="topbar__actions">
        {/* 알림. 숨겨둔 동안은 종 아이콘을 그리지 않는다 */}

        {SHOW_NOTIFICATIONS && (
          <div className="topbar__bell-wrap">
            <button type="button" className="topbar__icon-btn" onClick={() => togglePanel('bell')}>
              <Icon name="bell" size={21} />

              {unreadCount > 0 && <span className="badge topbar__count">{unreadCount}</span>}
            </button>

            {/* 알림창 */}

            {open === 'bell' && (
              <div className="topbar__panel topbar__panel--bell">
                <p className="topbar__panel-title">알림</p>

                {/* 알림 기능을 꺼놓은 경우 */}

                {!notify && (
                  <p className="topbar__panel-foot muted">
                    알림을 꺼두셨습니다.
                    <Link to="/settings">설정</Link>
                    에서 다시 켤 수 있습니다.
                  </p>
                )}

                {/* 알림은 켜져 있지만 알림이 없는 경우 */}

                {notify && notifications.length === 0 && (
                  <p className="topbar__panel-foot muted">아직 알림이 없습니다.</p>
                )}

                {/* 알림이 있는 경우 */}

                {notify && notifications.length > 0 && (
                  <ul className="topbar__notis">
                    {notifications.map((noti) => (
                      <li key={noti.id} className={isUnread(noti) ? 'is-unread' : ''}>
                        <span className="topbar__noti-icon">{noti.icon}</span>

                        <Link to={noti.link} className="topbar__noti-body" onClick={closePanel}>
                          <strong>{noti.title}</strong>

                          <span className="muted">{noti.body}</span>

                          <span className="topbar__noti-date muted">{formatDate(noti.createdAt)}</span>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        )}

        {/* =========================
            찜
           ========================= */}

        <Link to="/liked" className="topbar__icon-btn">
          <Icon name="heart" size={21} />

          {likeCount > 0 && <span className="badge topbar__count">{likeCount}</span>}
        </Link>

        {/* =========================
            로그인 사용자
           ========================= */}

        {user && (
          <div className="topbar__profile">
            <button type="button" className="topbar__profile-btn" onClick={() => togglePanel('profile')}>
              {/* 개인 이미지 파일 들어가는 곳  */}
              <span className="topbar__avatar">
                <img src={profileImage} />
              </span>
              {/* <span className="topbar__avatar">{user.nickname.slice(0, 1)}</span> */}
              {/* 앱 모드에서는 이 줄을 통째로 숨긴다 (styles/index.css 의 topbar__who) */}
              <span className="topbar__who">
                <span className="topbar__name">안녕하세요, {user.nickname}님!</span>
              </span>
              <span className="topbar__caret">⌄</span>
            </button>

            {/* 프로필 메뉴 */}

            {open === 'profile' && (
              <div className="topbar__panel topbar__menu">
                <Link to="/mypage" onClick={closePanel}>
                  MyPage
                </Link>

                <Link to="/history" onClick={closePanel}>
                  History
                </Link>

                <Link to="/ai-history" onClick={closePanel}>
                  AI 추천 기록
                </Link>

                <Link to="/settings" onClick={closePanel}>
                  설정
                </Link>

                <button type="button" onClick={handleLogout}>
                  로그아웃
                </button>
              </div>
            )}
          </div>
        )}

        {/* =========================
            로그인하지 않은 사용자
           ========================= */}

        {!user && (
          <Link to="/login" className="btn btn--sm">
            로그인
          </Link>
        )}
      </div>

      {/* 메뉴 바깥쪽을 누르면 닫기 */}

      {open && <button type="button" className="topbar__backdrop" onClick={closePanel} />}
    </header>
  )
}
