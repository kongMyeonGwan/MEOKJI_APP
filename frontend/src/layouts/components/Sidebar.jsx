import { Link, NavLink } from 'react-router-dom'
import Icon, { LogoMark } from '../../components/common/Icon'
import './Sidebar.css'

// 왼쪽 사이드바. 모바일에서는 상단바의 ☰ 를 누르면 서랍처럼 열린다.
// auth: true 인 메뉴는 로그인했을 때만 보인다.

const MAIN_MENU = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/recommend', label: '오늘의 추천', icon: 'sparkles' },
  { to: '/game', label: '메뉴 게임', icon: 'gamepad', badge: 'NEW' },
  { to: '/budget', label: '예산 맞춤 찾기', icon: 'wallet' },
  { to: '/search', label: '레시피 검색', icon: 'search' },
  { to: '/mypage', label: '못 먹는 재료', icon: 'ban', auth: true },
]

const SUB_MENU = [
  { to: '/history', label: 'History', icon: 'clock', auth: true },
  { to: '/liked', label: '찜한 레시피', icon: 'heart' },
  { to: '/community', label: '커뮤니티', icon: 'chat' },
  { to: '/ai-history', label: 'AI 추천 기록', icon: 'robot', auth: true },
  { to: '/settings', label: '설정', icon: 'gear' },
]

export default function Sidebar({
  onNavigate, // 메뉴를 누르면 부르는 함수 (모바일에서 서랍 닫기)
  user,
  theme,
  onThemeChange,
  notify,
  onNotifyChange,
}) {
  // 메뉴 한 줄 그리기. 로그인이 필요한 메뉴는 로그인 안 했으면 숨긴다.
  const renderItem = (item) => {
    if (item.auth && !user) {
      return null
    }

    return (
      <NavLink
        key={item.to}
        to={item.to}
        end={item.to === '/'}
        onClick={onNavigate}
        // NavLink 는 지금 주소와 같으면 isActive 가 true 다
        className={({ isActive }) => (isActive ? 'sidebar__item is-active' : 'sidebar__item')}
      >
        <Icon name={item.icon} className="sidebar__icon" />
        <span className="sidebar__label">{item.label}</span>
        {item.badge && <span className="sidebar__badge">{item.badge}</span>}
      </NavLink>
    )
  }

  return (
    <aside className="sidebar">
      <Link to="/" className="sidebar__logo" onClick={onNavigate}>
        <LogoMark size={34} />
        오늘 뭐 먹지?
      </Link>

      <nav className="sidebar__nav">
        {MAIN_MENU.map(renderItem)}
        <hr className="sidebar__divider" />
        {SUB_MENU.map(renderItem)}
      </nav>

      {/* 아래쪽 버튼들: 설정, 알림 켜기/끄기, 밝게, 어둡게 */}
      <div className="sidebar__foot">
        <div className="sidebar__utils">
          <Link to="/settings" className="sidebar__util" title="설정" onClick={onNavigate}>
            <Icon name="gear" size={19} />
          </Link>

          <button
            type="button"
            className={notify ? 'sidebar__util is-on' : 'sidebar__util'}
            onClick={() => onNotifyChange(!notify)}
            title={notify ? '알림 켜짐' : '알림 꺼짐'}
          >
            <Icon name="bell" size={19} />
          </button>

          <button
            type="button"
            className={theme === 'light' ? 'sidebar__util is-on' : 'sidebar__util'}
            onClick={() => onThemeChange('light')}
            title="라이트 모드"
          >
            <Icon name="sun" size={19} />
          </button>

          <button
            type="button"
            className={theme === 'dark' ? 'sidebar__util is-on' : 'sidebar__util'}
            onClick={() => onThemeChange('dark')}
            title="다크 모드"
          >
            <Icon name="moon" size={19} />
          </button>
        </div>
      </div>
    </aside>
  )
}
