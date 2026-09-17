import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { usePrefs } from '../../contexts/PrefsContext'
import Icon, { LogoMark } from './Icon'
import '../../styles/components/common/Sidebar.css'

// 왼쪽 사이드바 (1024px 이상에서만 보인다).
// 모바일에서는 하단 탭이 같은 역할을 하고, 상단바의 ☰ 를 누르면
// 이 사이드바가 서랍처럼 열린다.
//
// auth: true 인 메뉴는 로그인해야 열리는 화면이라 Guest 한테는 숨긴다.

export const NAV_PRIMARY = [
  { to: '/', label: '홈', icon: 'home', end: true },
  { to: '/recommend', label: '오늘의 추천', icon: 'sparkles' },
  { to: '/game', label: '메뉴 게임', icon: 'gamepad', badge: 'NEW' },
  { to: '/budget', label: '예산 맞춤 찾기', icon: 'wallet' },
  { to: '/search', label: '레시피 검색', icon: 'search' },
  { to: '/mypage', label: '못 먹는 재료', icon: 'ban', auth: true },
]

export const NAV_SECONDARY = [
  { to: '/history', label: 'History', icon: 'clock', auth: true },
  { to: '/liked', label: '찜한 레시피', icon: 'heart' },
  { to: '/community', label: '커뮤니티', icon: 'chat' },
  { to: '/ai-history', label: 'AI 추천 기록', icon: 'robot', auth: true },
  { to: '/settings', label: '설정', icon: 'gear' },
]

// 메뉴 한 줄
function NavItem({ item, onNavigate }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      onClick={onNavigate}
      className={function (state) {
        if (state.isActive) {
          return 'sidebar__item is-active'
        }
        return 'sidebar__item'
      }}
    >
      <Icon name={item.icon} className="sidebar__icon" />
      <span className="sidebar__label">{item.label}</span>
      {item.badge && <span className="sidebar__badge">{item.badge}</span>}
    </NavLink>
  )
}

// onNavigate 는 모바일에서 서랍으로 열렸을 때 메뉴를 누르면 서랍을 닫는 함수다.
// PC 에서는 안 넘어오기 때문에 아무 일도 안 한다.
export default function Sidebar({ onNavigate }) {
  const { isAuthenticated } = useAuth()
  const { theme, setTheme, notify, setNotify } = usePrefs()

  // 지금 보여줘도 되는 메뉴만 남긴다.
  // 로그인 안 했는데 보여주면 눌렀을 때 /login 으로 튕겨서
  // 눌러보기 전에는 왜 안 열리는지 알 수 없는 메뉴가 된다.
  function visible(items) {
    const result = []

    for (let i = 0; i < items.length; i++) {
      if (!items[i].auth || isAuthenticated) {
        result.push(items[i])
      }
    }

    return result
  }

  // 알림 버튼 색
  let notifyClass = 'sidebar__util'
  let notifyTitle = '알림 꺼짐'
  if (notify) {
    notifyClass = 'sidebar__util is-on'
    notifyTitle = '알림 켜짐'
  }

  // 테마 버튼 색
  let lightClass = 'sidebar__util'
  if (theme === 'light') {
    lightClass = 'sidebar__util is-on'
  }

  let darkClass = 'sidebar__util'
  if (theme === 'dark') {
    darkClass = 'sidebar__util is-on'
  }

  return (
    <aside className="sidebar">
      <Link to="/" className="sidebar__logo" onClick={onNavigate}>
        <LogoMark size={34} />
        오늘 뭐 먹지?
      </Link>

      <nav className="sidebar__nav" aria-label="주요 메뉴">
        {visible(NAV_PRIMARY).map(function (item) {
          return <NavItem key={item.to} item={item} onNavigate={onNavigate} />
        })}

        <hr className="sidebar__divider" />

        {visible(NAV_SECONDARY).map(function (item) {
          return <NavItem key={item.to} item={item} onNavigate={onNavigate} />
        })}
      </nav>

      {/* CSS 의 margin-top:auto 때문에 아래쪽에 붙는다 */}
      <div className="sidebar__foot">
        {/* 자주 바꾸는 스위치만 아이콘으로 내놓는다.
            설정 화면에도 같은 게 있지만 테마처럼 자주 바꾸는 걸
            화면을 옮겨가서 바꾸면 번거롭다. */}
        <div className="sidebar__utils">
          <Link
            to="/settings"
            className="sidebar__util"
            title="설정"
            aria-label="설정"
            onClick={onNavigate}
          >
            <Icon name="gear" size={19} />
          </Link>

          <button
            type="button"
            className={notifyClass}
            onClick={() => setNotify(!notify)}
            title={notifyTitle}
          >
            <Icon name="bell" size={19} />
          </button>

          <button
            type="button"
            className={lightClass}
            onClick={() => setTheme('light')}
            title="라이트 모드"
          >
            <Icon name="sun" size={19} />
          </button>

          <button
            type="button"
            className={darkClass}
            onClick={() => setTheme('dark')}
            title="다크 모드"
          >
            <Icon name="moon" size={19} />
          </button>
        </div>
      </div>
    </aside>
  )
}
