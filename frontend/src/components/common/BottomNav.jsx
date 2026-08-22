import { NavLink } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Icon from './Icon'
import '../../styles/components/common/BottomNav.css'

/**
 * 모바일 전용 하단 탭 (1023px 이하에서만 보인다).
 *
 * 사이드바의 메뉴가 7개라 하단 탭에 다 넣을 수 없다.
 * 탭은 5개를 넘기면 누르기 어려워지므로 자주 쓰는 것만 남기고,
 * 나머지는 MY(마이페이지)를 통해 들어가게 한다.
 */

const GUEST_TABS = [
  { to: '/', label: '홈', icon: 'home', end: true },
  { to: '/recommend', label: '추천', icon: 'sparkles' },
  { to: '/liked', label: '좋아요', icon: 'heart' },
  { to: '/community', label: '커뮤니티', icon: 'chat' },
  { to: '/login', label: '로그인', icon: 'user' },
]

const USER_TABS = [
  { to: '/', label: '홈', icon: 'home', end: true },
  { to: '/recommend', label: '추천', icon: 'sparkles' },
  { to: '/liked', label: '좋아요', icon: 'heart' },
  { to: '/history', label: 'History', icon: 'clock' },
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
