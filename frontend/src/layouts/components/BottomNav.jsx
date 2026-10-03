import { NavLink } from 'react-router-dom'
import Icon from '../../components/common/Icon'

// 모바일에서만 보이는 하단 탭. 로그인 여부에 따라 탭이 달라진다.

const GUEST_TABS = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/recommend', label: '추천', icon: 'sparkles' },
  { to: '/game', label: '게임', icon: 'gamepad' },
  { to: '/community', label: '커뮤니티', icon: 'chat' },
  { to: '/login', label: '로그인', icon: 'user' },
]

const USER_TABS = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/recommend', label: '추천', icon: 'sparkles' },
  { to: '/game', label: '게임', icon: 'gamepad' },
  { to: '/liked', label: '찜', icon: 'heart' },
  { to: '/mypage', label: 'MY', icon: 'user' },
]

export default function BottomNav({ user }) {
  const tabs = user ? USER_TABS : GUEST_TABS

  return (
    <nav className="bottom-nav">
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          // end 가 없으면 '/' 탭이 모든 화면에서 켜진다
          end={tab.to === '/'}
          className={({ isActive }) => (isActive ? 'bottom-nav__item is-active' : 'bottom-nav__item')}
        >
          <Icon name={tab.icon} size={22} />
          <span className="bottom-nav__label">{tab.label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
