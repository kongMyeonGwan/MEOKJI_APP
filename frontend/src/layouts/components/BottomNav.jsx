import { NavLink } from 'react-router-dom'
import Icon from '../../components/common/Icon'

export default function BottomNav({ user }) {
  // NavLink 는 지금 주소와 같으면 isActive 가 true 다.
  // 그때 is-active 를 붙여서 지금 보고 있는 탭을 강조한다 (styles/index.css)
  const tabClass = ({ isActive }) => (isActive ? 'bottom-nav__item is-active' : 'bottom-nav__item')

  return (
    <nav className="bottom-nav">
      {/* 홈 */}
      <NavLink to="/" end className={tabClass}>
        <Icon name="home" size={22} />
        <span className="bottom-nav__label">홈</span>
      </NavLink>

      {/* 추천 */}
      <NavLink to="/recommend" className={tabClass}>
        <Icon name="sparkles" size={22} />
        <span className="bottom-nav__label">추천</span>
      </NavLink>

      {/* 게임 */}
      <NavLink to="/game" className={tabClass}>
        <Icon name="gamepad" size={22} />
        <span className="bottom-nav__label">게임</span>
      </NavLink>

      {/* 로그인한 경우 */}
      {user && (
        <>
          <NavLink to="/liked" className={tabClass}>
            <Icon name="heart" size={22} />
            <span className="bottom-nav__label">찜</span>
          </NavLink>

          <NavLink to="/mypage" className={tabClass}>
            <Icon name="user" size={22} />
            <span className="bottom-nav__label">MY</span>
          </NavLink>
        </>
      )}

      {/* 로그인하지 않은 경우 */}
      {!user && (
        <>
          <NavLink to="/community" className={tabClass}>
            <Icon name="chat" size={22} />
            <span className="bottom-nav__label">커뮤니티</span>
          </NavLink>

          <NavLink to="/login" className={tabClass}>
            <Icon name="user" size={22} />
            <span className="bottom-nav__label">로그인</span>
          </NavLink>
        </>
      )}
    </nav>
  )
}
