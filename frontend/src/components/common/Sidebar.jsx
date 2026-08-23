import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { usePrefs } from '../../contexts/PrefsContext'
import Icon, { LogoMark } from './Icon'
import { MOCK_PROFILE } from '../../mocks'
import '../../styles/components/common/Sidebar.css'

/**
 * 좌측 사이드바 (웹 모드 전용, 1024px 이상).
 *
 * 앱 모드에서는 하단 탭(BottomNav)이 같은 역할을 하고, 상단바의 ☰ 를 누르면
 * 이 사이드바가 서랍으로 열린다(MainLayout 참고).
 *
 * 메뉴 아래에는 요리 레벨 카드를 붙였다. 최근 기록을 여기 두던 것을 뺐는데,
 * 기록은 History 메뉴로 들어가면 전부 볼 수 있는 반면 레벨은 들어갈 화면이 없어서
 * 어딘가 상시로 보여줄 자리가 필요했다. 게임·연속 방문 같은 장치가
 * "오늘도 들어올 이유"를 만드는 쪽이라 사이드바 하단이 그 자리로 맞다.
 *
 * `auth: true` 항목은 로그인해야 열리는 화면이라 Guest에게는 숨긴다.
 * 보여줘도 누르는 순간 ProtectedRoute가 /login으로 돌려보내기 때문에
 * 눌러보기 전에는 알 수 없는 메뉴가 된다. BottomNav도 같은 규칙이다.
 */

export const NAV_PRIMARY = [
  { to: '/', label: '홈', icon: 'home', end: true },
  { to: '/recommend', label: '오늘의 추천', icon: 'sparkles' },
  { to: '/game', label: '메뉴 게임', icon: 'gamepad', badge: 'NEW' },
  { to: '/budget', label: '예산 맞춤 찾기', icon: 'wallet' },
  { to: '/search', label: '레시피 검색', icon: 'search' },
  { to: '/mypage', label: '못 먹는 재료', icon: 'ban', auth: true },
  { to: '/fridge', label: '냉장고 재료로 찾기', icon: 'fridge' },
]

export const NAV_SECONDARY = [
  { to: '/history', label: 'History', icon: 'clock', auth: true },
  { to: '/liked', label: '찜한 레시피', icon: 'heart' },
  { to: '/community', label: '커뮤니티', icon: 'chat' },
  { to: '/ai-history', label: 'AI 추천 기록', icon: 'robot', auth: true },
  { to: '/settings', label: '설정', icon: 'gear' },
]

function NavItem({ item, onNavigate }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      onClick={onNavigate}
      className={({ isActive }) => `sidebar__item ${isActive ? 'is-active' : ''}`}
    >
      <Icon name={item.icon} className="sidebar__icon" />
      <span className="sidebar__label">{item.label}</span>
      {item.badge && <span className="sidebar__badge">{item.badge}</span>}
    </NavLink>
  )
}

/**
 * 요리 레벨 카드.
 *
 * 경험치 막대는 서버가 내려준 exp / exp_next 로 그 자리에서 계산한다.
 * 퍼센트를 서버에서 받아오면 "320 / 500" 같은 원래 숫자를 못 보여준다.
 */
function LevelCard({ onNavigate }) {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return (
      <div className="level level--guest">
        <span className="level__icon" aria-hidden="true">
          🏆
        </span>
        <p className="level__guest-text">
          <Link to="/login" onClick={onNavigate}>
            로그인
          </Link>
          하면 요리 레벨이 쌓여요.
        </p>
      </div>
    )
  }

  const { level, title, exp, exp_next: expNext, streak_days: streak } = MOCK_PROFILE
  const percent = Math.min(100, Math.round((exp / expNext) * 100))

  return (
    <div className="level">
      <div className="level__head">
        <span className="level__icon" aria-hidden="true">
          🏆
        </span>
        <div>
          <h2 className="level__title">나의 요리 레벨</h2>
          <p className="level__sub">
            Lv.{level} {title}
          </p>
        </div>
      </div>

      <div
        className="level__bar"
        role="progressbar"
        aria-valuenow={exp}
        aria-valuemin={0}
        aria-valuemax={expNext}
        aria-label="다음 레벨까지 남은 경험치"
      >
        <span className="level__fill" style={{ width: `${percent}%` }} />
      </div>
      <p className="level__exp">
        {exp.toLocaleString('ko-KR')} / {expNext.toLocaleString('ko-KR')} EXP
      </p>

      <dl className="level__stats">
        <div>
          <dt>연속 방문</dt>
          <dd>{streak}일</dd>
        </div>
        <div>
          <dt>추천 완료</dt>
          <dd>{MOCK_PROFILE.recommend_count}회</dd>
        </div>
        <div>
          <dt>요리 완료</dt>
          <dd>{MOCK_PROFILE.cook_count}회</dd>
        </div>
      </dl>

      <Link
        to="/history"
        className="btn btn--ghost btn--sm btn--block level__more"
        onClick={onNavigate}
      >
        내 활동 보기 ›
      </Link>
    </div>
  )
}

/**
 * onNavigate — 앱 모드에서 서랍으로 열렸을 때 메뉴를 누르면 서랍을 닫는다.
 * 웹 모드에서는 넘어오지 않으므로 아무 일도 하지 않는다.
 */
export default function Sidebar({ onNavigate }) {
  const { isAuthenticated } = useAuth()
  const { theme, setTheme, notify, setNotify } = usePrefs()

  const visible = (items) => items.filter((item) => !item.auth || isAuthenticated)

  return (
    <aside className="sidebar">
      <Link to="/" className="sidebar__logo" onClick={onNavigate}>
        <LogoMark size={34} />
        오늘 뭐 먹지?
      </Link>

      <nav className="sidebar__nav" aria-label="주요 메뉴">
        {visible(NAV_PRIMARY).map((item) => (
          <NavItem key={item.to} item={item} onNavigate={onNavigate} />
        ))}

        <hr className="sidebar__divider" />

        {visible(NAV_SECONDARY).map((item) => (
          <NavItem key={item.to} item={item} onNavigate={onNavigate} />
        ))}
      </nav>

      {/* margin-top:auto 로 아래쪽에 붙는다. 메뉴가 길어지면 함께 밀려난다 */}
      <div className="sidebar__foot">
        <LevelCard onNavigate={onNavigate} />

        {/*
         * 자주 바꾸는 스위치만 아이콘으로 내놓는다.
         * 설정 화면에도 같은 항목이 있지만, 테마처럼 하루에도 몇 번씩 바꾸는 것을
         * 화면을 옮겨서 바꾸게 하면 번거롭다.
         */}
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
            className={`sidebar__util${notify ? ' is-on' : ''}`}
            onClick={() => setNotify(!notify)}
            aria-pressed={notify}
            title={notify ? '알림 켜짐' : '알림 꺼짐'}
          >
            <Icon name="bell" size={19} />
          </button>

          <button
            type="button"
            className={`sidebar__util${theme === 'light' ? ' is-on' : ''}`}
            onClick={() => setTheme('light')}
            aria-pressed={theme === 'light'}
            title="라이트 모드"
          >
            <Icon name="sun" size={19} />
          </button>

          <button
            type="button"
            className={`sidebar__util${theme === 'dark' ? ' is-on' : ''}`}
            onClick={() => setTheme('dark')}
            aria-pressed={theme === 'dark'}
            title="다크 모드"
          >
            <Icon name="moon" size={19} />
          </button>
        </div>
      </div>
    </aside>
  )
}
