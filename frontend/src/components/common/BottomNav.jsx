import { NavLink } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Icon from './Icon'
import '../../styles/components/common/BottomNav.css'

// 모바일에서만 보이는 하단 탭 (1023px 이하).
//
// 사이드바 메뉴가 열 개가 넘어서 하단 탭에 다 넣을 수 없다.
// 탭이 5개를 넘으면 누르기 어려워서 자주 쓰는 것만 남기고
// 나머지는 상단바의 ☰ 로 사이드바를 열어서 들어간다.

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

  let tabs = GUEST_TABS
  if (isAuthenticated) {
    tabs = USER_TABS
  }

  return (
    <nav className="bottom-nav" aria-label="주요 메뉴">
      {tabs.map(function (tab) {
        return (
          <NavLink
            key={tab.to}
            to={tab.to}
            // end 가 없으면 "/" 탭이 모든 화면에서 켜진 것처럼 보인다
            end={tab.end}
            className={function (state) {
              if (state.isActive) {
                return 'bottom-nav__item is-active'
              }
              return 'bottom-nav__item'
            }}
          >
            <Icon name={tab.icon} size={22} />
            <span className="bottom-nav__label">{tab.label}</span>
          </NavLink>
        )
      })}
    </nav>
  )
}
