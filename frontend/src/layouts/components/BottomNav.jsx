import { NavLink } from 'react-router-dom'
import Icon from '../../components/common/Icon'

// 아래쪽 탭. 앱 모드(1023px 이하)에서만 보인다 - 웹 모드에서는
// CSS 가 숨기고 사이드바가 그 일을 한다.
//
// 왜 사이드바와 따로 두나 — 탭은 다섯 개가 한계다. 좁은 화면에
// 더 넣으면 글자가 겹친다. 사이드바에는 아홉 개가 들어 있으므로
// 그 중에서 자주 쓰는 것만 골라 담았다.
//
// 그래서 메뉴를 하나 더할 때 두 파일을 다 봐야 한다. 사이드바에만
// 넣을지, 탭에도 넣을지(그러면 무엇을 뺄지)를 정해야 한다.
//
// 로그인 여부에 따라 뒤쪽 두 칸이 바뀐다. 개수는 늘 다섯이라
// 칸 너비가 흔들리지 않는다.
export default function BottomNav({ user }) {
  // NavLink 는 지금 주소와 같으면 isActive 가 true 다.
  // 그때 is-active 를 붙여서 지금 보고 있는 탭을 강조한다 (styles/index.css)
  //
  // 사이드바의 linkClass 와 같은 모양이지만 클래스 이름이 다르다.
  // 한 곳에 모으지 않은 이유 — 이름이 달라서 함수를 공유해도
  // 결국 이름을 인자로 받아야 하고, 그러면 더 길어진다
  const tabClass = ({ isActive }) => (isActive ? 'bottom-nav__item is-active' : 'bottom-nav__item')

  return (
    <nav className="bottom-nav">
      {/* 홈.
          end 가 붙어 있다. 없으면 "/" 가 모든 주소의 앞부분과 맞아서,
          어느 화면에 있든 홈 탭이 강조된다 */}
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

      {/* 로그인한 경우.
          <> </> 는 빈 태그다(Fragment). 두 개를 한 덩어리로 묶어야
          조건부로 그릴 수 있는데, div 로 감싸면 그 div 가 탭 하나처럼
          자리를 차지해 칸 너비가 틀어진다 */}
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

      {/* 로그인하지 않은 경우.
          찜 · MY 자리에 커뮤니티 · 로그인이 들어간다.
          둘 다 로그인 없이 볼 수 있는 자리다 */}
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
