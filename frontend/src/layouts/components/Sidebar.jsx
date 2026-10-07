import { Link, NavLink } from 'react-router-dom'
import Icon, { LogoMark } from '../../components/common/Icon'

export default function Sidebar({ onNavigate, user, theme, onThemeChange, notify, onNotifyChange }) {
  // NavLink 는 지금 주소와 같으면 isActive 가 true 다.
  // 그때 is-active 를 붙여서 지금 보고 있는 메뉴를 강조한다 (styles/index.css)
  const linkClass = ({ isActive }) => (isActive ? 'sidebar__item is-active' : 'sidebar__item')

  return (
    <aside className="sidebar">
      {/* 로고 */}
      <Link to="/" className="sidebar__logo" onClick={onNavigate}>
        <LogoMark size={34} />
        오늘 뭐 먹지?
      </Link>

      {/* 메뉴 */}
      <nav className="sidebar__nav">
        {/* 홈 */}
        <NavLink to="/" end className={linkClass} onClick={onNavigate}>
          <Icon name="home" className="sidebar__icon" />
          <span className="sidebar__label">홈</span>
        </NavLink>

        {/* 오늘의 추천 */}
        <NavLink to="/recommend" className={linkClass} onClick={onNavigate}>
          <Icon name="sparkles" className="sidebar__icon" />
          <span className="sidebar__label">오늘의 추천</span>
        </NavLink>

        {/* 메뉴 게임 */}
        <NavLink to="/game" className={linkClass} onClick={onNavigate}>
          <Icon name="gamepad" className="sidebar__icon" />
          <span className="sidebar__label">메뉴 게임</span>
          <span className="sidebar__badge">NEW</span>
        </NavLink>

        {/* 예산 맞춤 찾기 */}
        {/* <NavLink to="/budget" className={linkClass} onClick={onNavigate}>
          <Icon name="wallet" className="sidebar__icon" />
          <span className="sidebar__label">예산 맞춤 찾기</span>
        </NavLink> */}

        {/* 레시피 검색 */}
        {/* <NavLink to="/search" className={linkClass} onClick={onNavigate}>
          <Icon name="search" className="sidebar__icon" />
          <span className="sidebar__label">레시피 검색</span>
        </NavLink> */}

        {/* 로그인했을 때만 보이는 메뉴 */}
        {user && (
          <NavLink to="/mypage" className={linkClass} onClick={onNavigate}>
            <Icon name="ban" className="sidebar__icon" />
            <span className="sidebar__label">못 먹는 재료</span>
          </NavLink>
        )}

        <hr className="sidebar__divider" />

        {/* 로그인했을 때만 History 표시 */}
        {user && (
          <NavLink to="/history" className={linkClass} onClick={onNavigate}>
            <Icon name="clock" className="sidebar__icon" />
            <span className="sidebar__label">History</span>
          </NavLink>
        )}

        {/* 찜한 레시피 */}
        <NavLink to="/liked" className={linkClass} onClick={onNavigate}>
          <Icon name="heart" className="sidebar__icon" />
          <span className="sidebar__label">찜한 레시피</span>
        </NavLink>

        <NavLink to="/reviews" className={linkClass} onClick={onNavigate}>
          <Icon name="search" className="sidebar__icon" />
          <span className="sidebar__label">레시피 추천</span>
        </NavLink>

        {/* 커뮤니티 */}
        <NavLink to="/community" className={linkClass} onClick={onNavigate}>
          <Icon name="chat" className="sidebar__icon" />
          <span className="sidebar__label">커뮤니티</span>
        </NavLink>

        {/* 인기 레시피 */}
        <NavLink to="/popular" className={linkClass} onClick={onNavigate}>
          <Icon name="fire" className="sidebar__icon" />
          <span className="sidebar__label">인기 레시피</span>
        </NavLink>

        {/* 로그인했을 때만 AI 추천 기록 표시 */}
        {user && (
          <NavLink to="/ai-history" className={linkClass} onClick={onNavigate}>
            <Icon name="robot" className="sidebar__icon" />
            <span className="sidebar__label">AI 추천 기록</span>
          </NavLink>
        )}

        {/* 설정 */}
        <NavLink to="/settings" className={linkClass} onClick={onNavigate}>
          <Icon name="gear" className="sidebar__icon" />
          <span className="sidebar__label">설정</span>
        </NavLink>
      </nav>

      {/* 사이드바 아래 버튼 */}
      <div className="sidebar__foot">
        <div className="sidebar__utils">
          {/* 설정 */}
          <Link to="/settings" className="sidebar__util" title="설정" onClick={onNavigate}>
            <Icon name="gear" size={19} />
          </Link>

          {/* 알림 */}
          <button
            type="button"
            className={notify ? 'sidebar__util is-on' : 'sidebar__util'}
            title={notify ? '알림 켜짐' : '알림 꺼짐'}
            onClick={() => onNotifyChange(!notify)}
          >
            <Icon name="bell" size={19} />
          </button>

          {/* 라이트 모드 */}
          <button
            type="button"
            className={theme === 'light' ? 'sidebar__util is-on' : 'sidebar__util'}
            title="라이트 모드"
            onClick={() => onThemeChange('light')}
          >
            <Icon name="sun" size={19} />
          </button>

          {/* 다크 모드 */}
          <button
            type="button"
            className={theme === 'dark' ? 'sidebar__util is-on' : 'sidebar__util'}
            title="다크 모드"
            onClick={() => onThemeChange('dark')}
          >
            <Icon name="moon" size={19} />
          </button>
        </div>
      </div>
    </aside>
  )
}
