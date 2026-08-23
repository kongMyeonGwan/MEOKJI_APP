import { NavLink } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Icon from './Icon'
import '../../styles/components/common/BottomNav.css'

/**
 * 모바일 전용 하단 탭 (1023px 이하에서만 보인다).
 *
 * 사이드바의 메뉴가 열두 개라 하단 탭에 다 넣을 수 없다.
 * 탭은 5개를 넘기면 누르기 어려워지므로 자주 쓰는 것만 남기고,
 * 나머지는 상단바의 ☰ 로 사이드바를 열어 들어간다.
 *
 * 게임을 탭에 넣은 이유: 대시보드 배너로도 갈 수 있지만 스크롤을 해야 하고,
 * "못 정하겠을 때" 여는 것이라 어느 화면에 있든 한 번에 닿아야 한다.
 */

const GUEST_TABS = [
  { to: '/', label: '홈', icon: 'home', end: true },
  { to: '/recommend', label: '추천', icon: 'sparkles' },
  { to: '/game', label: '게임', icon: 'gamepad' },
  { to: '/community', label: '커뮤니티', icon: 'chat' },
  { to: '/login', label: '로그인', icon: 'user' },
]

const USER_TABS = [
  { to: '/', label: '홈', icon: 'home', end: true },
  { to: '/recommend', label: '추천', icon: 'sparkles' },
  { to: '/game', label: '게임', icon: 'gamepad' },
  { to: '/liked', label: '찜', icon: 'heart' },
  { to: '/mypage', label: 'MY', icon: 'user' },
]

export default function BottomNav() {
  const { isAuthenticated } = useAuth()
  const tabs = isAuthenticated ? USER_TABS : GUEST_TABS

  return (
    <nav className="bottom-nav" aria-label="주요 메뉴">
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          // end가 없으면 "/" 탭이 모든 경로에서 활성으로 표시된다
          end={tab.end}
          className={({ isActive }) =>
            `bottom-nav__item ${isActive ? 'is-active' : ''}`
          }
        >
          <Icon name={tab.icon} size={22} />
          <span className="bottom-nav__label">{tab.label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
