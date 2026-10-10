import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import Icon, { LogoMark } from '../../components/common/Icon'

import { getCatalog } from '../../api/catalog'
import { getLikes } from '../../api/likes'
import { getNotifications } from '../../api/users'

import { formatDate } from '../../utils/format'

// 위쪽 바. 메뉴 버튼 · 로고 · AI 검색 · 알림 · 찜 · 프로필이 있다.
//
// 이 컴포넌트만 서버를 직접 부른다(사이드바 · 하단탭은 안 부른다).
// 찜 개수와 알림은 화면 어디에 있든 보여야 하는 값이라, 화면마다
// 받아오게 하면 같은 요청이 화면 수만큼 생긴다.
//
// 열려 있는 메뉴를 state 하나(open)로 다룬다. 셋을 각각 true/false 로
// 두면 "하나를 열 때 나머지를 닫는" 처리를 손으로 해야 하고,
// 빠뜨리면 두 개가 같이 열린다.
//
// 알림 "읽음" 은 서버에 저장하지 않는다.
// 알림 창을 마지막으로 연 시각(readAt)을 브라우저에 저장해두고
// 그보다 나중에 온 알림을 "안 읽음" 으로 센다.
//
// 서버에 알림 표가 없어서다. 댓글과 좋아요를 그때그때 읽어 만들기
// 때문에 "읽음" 을 적어둘 자리가 없다. 기기마다 따로 세어지는 것이
// 흠이지만, 표를 하나 더 만드는 것보다 가볍다고 봤다.
const READ_AT_KEY = 'meokji.notify.readAt'

export default function TopBar({ onToggleNav, user, onLogout, notify }) {
  // ---------------------------------------
  // state
  // ---------------------------------------

  // 현재 열려있는 메뉴
  // null      : 아무것도 안 열림
  // 'ai'      : AI 검색
  // 'bell'    : 알림
  // 'profile' : 프로필
  //
  // 글자 하나로 두면 "한 번에 하나만 열린다" 가 저절로 지켜진다
  const [open, setOpen] = useState(null)

  // AI 검색 추천 문장
  const [searchSuggestions, setSearchSuggestions] = useState([])

  // 찜 개수
  const [likeCount, setLikeCount] = useState(0)

  // 알림 목록
  const [notifications, setNotifications] = useState([])

  // 알림 창을 마지막으로 연 시각
  //
  // localStorage 에 있는데 state 로도 들고 있는 이유 — 알림 창을
  // 열었을 때 안 읽은 개수가 바로 0 이 되어야 한다. localStorage 만
  // 바꾸면 리액트가 모르므로 화면이 안 바뀐다.
  //
  // || '' 로 빈 글자를 기본값으로 둔다. null 과 글자를 비교하면
  // (아래 isUnread) 결과가 늘 false 라 전부 "읽음" 이 된다
  const [readAt, setReadAt] = useState(localStorage.getItem(READ_AT_KEY) || '')

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

        setSearchSuggestions(data.search_suggestions || [])
      } catch (error) {
        // 실패해도 상단바는 그대로 뜬다. 추천 문장은 거들어주는
        // 값이라, 없으면 그 목록만 비어 보인다
        console.log('검색 추천 목록을 가져오지 못했습니다.')
      }
    }

    loadCatalog()
    // 처음 한 번만. 추천 문장은 서버가 파일에서 읽어 주는 고정값이라
    // 화면을 옮길 때마다 다시 받을 이유가 없다
  }, [])

  // ---------------------------------------
  // 로그인하면 찜 목록과 알림을 가져온다.
  // 화면을 옮길 때마다 다시 받아온다 - 찜을 누르고 돌아오면
  // 상단바 숫자도 같이 맞아야 한다.
  // ---------------------------------------

  useEffect(() => {
    // 로그인하지 않았으면 서버 요청 안 함
    //
    // 비우고 나가는 것이 중요하다. 로그아웃했을 때 앞사람의 찜 개수와
    // 알림이 그대로 남으면 안 된다
    if (!user) {
      setLikeCount(0)
      setNotifications([])

      return
    }

    const loadUserData = async () => {
      try {
        // 찜 목록
        const likes = await getLikes()

        setLikeCount(likes.count)

        // 알림 목록
        const notificationList = await getNotifications()

        setNotifications(notificationList)
      } catch (error) {
        console.log('사용자 정보를 가져오지 못했습니다.')
      }
    }

    loadUserData()
    // user 와 주소가 바뀔 때마다 다시 받는다.
    //
    // location.pathname 을 넣은 이유 — 찜을 누르고 다른 화면으로
    // 가면 상단바 숫자가 옛 값으로 남는다. 화면을 옮길 때 맞춘다.
    //
    // 대신 같은 화면에 머무는 동안에는 안 맞는다(찜을 눌러도 숫자가
    // 그대로다). 화면이 그 숫자를 따로 들고 있어서 당장 티는 안 난다
  }, [user, location.pathname])

  // ---------------------------------------
  // 안 읽은 알림
  // ---------------------------------------

  // 마지막으로 알림 창을 연 뒤에 온 알림인지
  //
  // 글자끼리 > 로 비교한다. created_at 이
  // "2026-10-04T12:30:00Z" 모양이라 자리가 고정이고,
  // 글자 순서가 곧 시간 순서다 - Date 로 바꿔 견줄 필요가 없다
  const isUnread = (noti) => {
    return noti.created_at > readAt
  }

  // 안 읽은 알림 개수. 알림을 꺼뒀으면 0
  //
  // 꺼뒀을 때 0 으로 두는 이유 — 알림을 껐는데 빨간 숫자가 붙어
  // 있으면 끈 것 같지 않다
  const unreadCount = notify ? notifications.filter(isUnread).length : 0

  // ---------------------------------------
  // 메뉴 열기 / 닫기
  // ---------------------------------------

  const togglePanel = (name) => {
    // 이미 열려있는 메뉴를 다시 누르면 닫기
    //
    // 이것이 없으면 열린 메뉴의 버튼을 또 눌러도 안 닫힌다.
    // 바깥을 눌러야만 닫히는데, 버튼을 누른 사람은 닫히기를 기대한다
    if (open === name) {
      setOpen(null)
      return
    }

    // 다른 메뉴 열기
    setOpen(name)

    // 알림 창을 열면 지금 시각을 "여기까지 읽음" 으로 저장한다
    //
    // 닫을 때가 아니라 열 때 적는다. 닫을 때 적으면 창을 열어둔
    // 사이에 온 알림까지 읽은 것이 된다
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
    //
    // 먼저 닫는다. 화면을 옮긴 뒤에 닫으려 하면 이 컴포넌트가
    // 그대로 남아 있어서 메뉴가 열린 채로 보인다
    closePanel()

    // 검색 페이지로 이동
    //
    // Link 가 아니라 navigate 를 쓰는 이유 — 누른 뒤에 메뉴를
    // 닫는 일도 같이 해야 한다. Link 는 바로 옮겨버린다
    //
    // encodeURIComponent 로 감싼다. 검색어에 한글과 공백이 들어 있어서
    // 그대로 주소에 넣으면 요청이 깨진다
    navigate('/search?q=' + encodeURIComponent(text))
  }

  // ---------------------------------------
  // 로그아웃
  // ---------------------------------------

  const handleLogout = () => {
    // 프로필 메뉴 닫기
    closePanel()

    // App.jsx의 로그아웃 함수 실행
    //
    // 여기서 토큰을 직접 지우지 않는다. App 이 들고 있는 user 도
    // 같이 바뀌어야 하는데, 그 상태는 App 에 있다
    onLogout()

    // 메인 페이지 이동
    //
    // 마이페이지처럼 로그인해야 하는 화면에 있다가 로그아웃하면,
    // 그 자리에 남아 있을 수 없다
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

        {/* 앱 모드에서는 CSS 가 이 글자를 숨긴다. 좁은 화면에서
            로고 글자까지 두면 오른쪽 아이콘이 밀린다 */}
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
        {/* 알림 */}

        <div className="topbar__bell-wrap">
          <button type="button" className="topbar__icon-btn" onClick={() => togglePanel('bell')}>
            <Icon name="bell" size={21} />

            {/* 0 일 때 안 그린다. {unreadCount && ...} 로 적으면
                0 이 그대로 화면에 찍힌다 - 리액트는 0 을 "안 그림" 이
                아니라 "숫자 0" 으로 보기 때문이다. > 0 으로 참/거짓을
                만들어야 한다 */}
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

                        <span className="topbar__noti-date muted">{formatDate(noti.created_at)}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

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
              {/* 닉네임 첫 글자를 동그라미에 넣는다. 사진이 없어서
                  대신 쓰는 것이다 */}
              <span className="topbar__avatar">{user.nickname.slice(0, 1)}</span>

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

      {/* 메뉴 바깥쪽을 누르면 닫기.
          열려 있을 때만 그린다 - 늘 그려두면 투명한 판이 화면을 덮어
          아무것도 누를 수 없게 된다.

          div 가 아니라 button 인 이유 — 키보드로도 닫을 수 있어야 한다 */}

      {open && <button type="button" className="topbar__backdrop" onClick={closePanel} />}
    </header>
  )
}
