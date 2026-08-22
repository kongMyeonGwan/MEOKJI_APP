import { Link, NavLink } from 'react-router-dom'
import Icon, { LogoMark } from './Icon'
import '../../styles/components/common/Sidebar.css'

/**
 * 좌측 사이드바 (웹 모드 전용, 1024px 이상).
 *
 * 앱 모드에서는 CSS로 숨기고 하단 탭(BottomNav)이 같은 역할을 한다.
 */

export const NAV_PRIMARY = [
  { to: '/', label: '홈', icon: 'home', end: true },
  { to: '/recommend', label: '오늘 뭐 먹지?', icon: 'sparkles' },
  { to: '/budget', label: '예산 맞춤 추천', icon: 'wallet' },
  { to: '/mypage', label: '못 먹는 재료', icon: 'ban' },
]

export const NAV_SECONDARY = [
  { to: '/history', label: 'History', icon: 'clock' },
  { to: '/liked', label: '좋아요 받은 레시피', icon: 'heart' },
  { to: '/community', label: '커뮤니티', icon: 'chat' },
]

function NavItem({ item }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) => `sidebar__item ${isActive ? 'is-active' : ''}`}
    >
      <Icon name={item.icon} className="sidebar__icon" />
      {item.label}
    </NavLink>
  )
}

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <Link to="/" className="sidebar__logo">
        <LogoMark size={28} />
        뭐먹지?
      </Link>

      <nav className="sidebar__nav" aria-label="주요 메뉴">
        {NAV_PRIMARY.map((item) => (
          <NavItem key={item.to} item={item} />
        ))}

        <hr className="sidebar__divider" />

        {NAV_SECONDARY.map((item) => (
          <NavItem key={item.to} item={item} />
        ))}
      </nav>
    </aside>
  )
}
