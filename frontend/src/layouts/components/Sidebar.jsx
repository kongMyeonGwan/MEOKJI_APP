import { Link, NavLink } from 'react-router-dom'
import Icon, { LogoMark } from '../../components/common/Icon'

// 왼쪽 메뉴.
//
// 웹 모드(1024px 이상)에서는 왼쪽에 고정돼 있고, 앱 모드에서는
// 숨어 있다가 메뉴 버튼을 누르면 밀려 나온다. 그 처리는 CSS 가 하고
// 여기서는 같은 것을 그린다.
//
// onNavigate 는 "메뉴를 눌렀다" 를 알리는 함수다. MainLayout 이
// 그것을 받아 앱 모드에서만 사이드바를 닫는다 - 여기서 직접 닫지
// 않는 이유는, 사이드바가 자기 자신을 열고 닫는 상태를 들고 있지
// 않기 때문이다(그 상태는 MainLayout 에 있다).
//
// 주석 처리된 메뉴가 넷 있다(예산 · 검색 · AI 기록). 화면은 만들어
// 두었지만 아직 열지 않은 것들이다.
export default function Sidebar({ onNavigate, user, theme, onThemeChange, notify, onNotifyChange }) {
  // NavLink 는 지금 주소와 같으면 isActive 가 true 다.
  // 그때 is-active 를 붙여서 지금 보고 있는 메뉴를 강조한다 (styles/index.css)
  //
  // className 에 함수를 넘길 수 있는 것은 NavLink 만의 기능이다.
  // 보통 Link 에는 없다 - 그래서 메뉴는 Link 가 아니라 NavLink 를 쓴다
  //
  // 함수를 한 번만 만들어 아홉 군데에 넘긴다. 메뉴마다 적으면
  // 같은 줄이 아홉 벌 생기고, 한 곳에서 클래스 이름을 틀려도
  // 그 메뉴만 강조가 안 된다
  const linkClass = ({ isActive }) => (isActive ? 'sidebar__item is-active' : 'sidebar__item')

  return (
    <aside className="sidebar">
      {/* 로고. 누르면 홈으로 간다.
          NavLink 가 아니라 Link 인 이유 — 로고는 "지금 여기 있다" 를
          강조할 자리가 아니다 */}
      <Link to="/" className="sidebar__logo" onClick={onNavigate}>
        <LogoMark size={34} />
        오늘 뭐 먹지?
      </Link>

      {/* 메뉴 */}
      <nav className="sidebar__nav">
        {/* 홈.
            end 가 붙어 있다. 없으면 "/" 가 모든 주소의 앞부분과 맞아서,
            어느 화면에 있든 홈이 강조된다 */}
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
          {/* 새 기능 표시. 글자로 박아두었다 - 뗄 때 이 줄을 지운다 */}
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

        {/* 로그인했을 때만 보이는 메뉴.
            {조건 && <태그/>} 는 조건이 참일 때만 그린다는 뜻이다.
            user 가 null 이면 아무것도 안 그려진다.

            안 보이게 하는 것이 막는 것은 아니다. 주소를 직접 치면
            들어가지는데, 그쪽은 App.jsx 의 loginOnly 가 막는다 */}
        {user && (
          <NavLink to="/mypage" className={linkClass} onClick={onNavigate}>
            <Icon name="ban" className="sidebar__icon" />
            <span className="sidebar__label">못 먹는 재료</span>
          </NavLink>
        )}

        {/* 메뉴를 두 묶음으로 나누는 줄.
            위는 "메뉴를 고르는 것", 아래는 "내 기록과 커뮤니티" 다 */}
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
        {/* {user && (
          <NavLink to="/ai-history" className={linkClass} onClick={onNavigate}>
            <Icon name="robot" className="sidebar__icon" />
            <span className="sidebar__label">AI 추천 기록</span>
          </NavLink>
        )} */}

        {/* 설정 */}
        <NavLink to="/settings" className={linkClass} onClick={onNavigate}>
          <Icon name="gear" className="sidebar__icon" />
          <span className="sidebar__label">설정</span>
        </NavLink>
      </nav>

      {/* 사이드바 아래 버튼.
          설정 · 알림 · 테마를 작은 아이콘으로 모아둔 자리다.
          메뉴 목록에 섞어두면 "화면을 여는 것" 과 "값을 바꾸는 것" 이
          구별되지 않는다 */}
      <div className="sidebar__foot">
        <div className="sidebar__utils">
          {/* 설정 */}
          <Link to="/settings" className="sidebar__util" title="설정" onClick={onNavigate}>
            <Icon name="gear" size={19} />
          </Link>

          {/* 알림 */}
          {/* 누르면 켜고 끄기가 뒤집힌다.
              !notify 를 넘기는 이유 — 지금 값의 반대가 바꿀 값이다.

              type="button" 을 꼭 적는다. 안 적으면 기본이 submit 이라,
              폼 안에 들어가면 누를 때 폼이 보내진다 */}
          <button
            type="button"
            className={notify ? 'sidebar__util is-on' : 'sidebar__util'}
            title={notify ? '알림 켜짐' : '알림 꺼짐'}
            onClick={() => onNotifyChange(!notify)}
          >
            <Icon name="bell" size={19} />
          </button>

          {/* 라이트 모드 · 다크 모드.
              알림처럼 뒤집지 않고 값을 직접 넘긴다 - 지금 켜진 쪽을
              또 눌러도 그대로여야 한다. 뒤집기로 두면 두 버튼이
              서로를 끄고 켜게 된다 */}
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
