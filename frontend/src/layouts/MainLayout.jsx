import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'

import Sidebar from './components/Sidebar'
import TopBar from './components/TopBar'
import BottomNav from './components/BottomNav'

// 모든 화면을 감싸는 틀. 사이드바 · 상단바 · 하단탭이 여기 있다.
//
// 화면마다 이 셋을 각각 그리면, 메뉴를 하나 더할 때 화면 수만큼
// 고쳐야 한다. 라우터에서 이 컴포넌트로 감싸두면 한 곳만 고친다.
//
// 받은 props 를 대부분 그대로 내려보낸다. 여기서 쓰는 것은 user 뿐이고
// (사이드바 · 상단바 · 하단탭이 다 쓴다), 나머지는 지나가는 값이다.
//
// 반응형이 두 모드다 — 1024px 이상이면 사이드바가 왼쪽에 고정되고,
// 그 아래면 사이드바가 숨고 하단탭이 나온다. 경계값을 CSS 와
// 이 파일 둘 다에 적어두었으므로 바꿀 때 같이 고쳐야 한다.
export default function MainLayout({ user, onLogout, theme, onThemeChange, notify, onNotifyChange }) {
  // 지금 주소를 알려준다. 아래 useEffect 가 "화면이 바뀌었는지" 를
  // 이 값으로 가린다
  const location = useLocation()

  // 사이드바 열기 / 닫기. 웹 모드(1024px 이상)는 연 채로 시작한다
  //
  // 처음 값으로 창 너비를 보는 이유 — 넓은 화면에서 닫힌 채로 시작하면
  // 사용자가 메뉴 버튼을 한 번 눌러야 한다. 좁은 화면에서 열린 채로
  // 시작하면 내용이 가려진다.
  //
  // 창 크기가 바뀌는 것은 따라가지 않는다. 그러려면 resize 를 듣고
  // 있어야 하는데, 도중에 크기를 바꾸는 일이 드물다
  const [navOpen, setNavOpen] = useState(window.innerWidth >= 1024)

  // 화면을 옮기면 스크롤을 맨 위로 올린다.
  // 목록을 한참 내려보다 글을 누르면 상세가 중간부터 보인다.
  useEffect(() => {
    window.scrollTo(0, 0)
    // location 이 아니라 location.pathname 을 보는 이유 —
    // location 은 객체라 주소가 같아도 매번 새것이 온다.
    // 그러면 화면이 다시 그려질 때마다 맨 위로 올라간다
  }, [location.pathname])

  // 사이드바 열기 / 닫기 버튼
  const toggleNav = () => {
    setNavOpen(!navOpen)
  }

  // 사이드바 닫기.
  // 앱 모드(1023px 이하)에서만 닫는다 — 웹 모드의 사이드바는 왼쪽에 고정이라
  // 메뉴를 눌렀다고 사라지면 안 된다.
  const closeNav = () => {
    if (window.innerWidth < 1024) {
      setNavOpen(false)
    }
  }

  return (
    // is-nav-open 을 바깥 div 에 붙인다. 사이드바만이 아니라
    // 본문 영역도 같이 밀려야 해서, CSS 가 이 클래스를 보고
    // 둘을 한꺼번에 움직인다
    <div className={navOpen ? 'shell is-nav-open' : 'shell'}>
      {/* 왼쪽 메뉴 */}
      <Sidebar
        user={user}
        theme={theme}
        notify={notify}
        onThemeChange={onThemeChange}
        onNotifyChange={onNotifyChange}
        onNavigate={closeNav}
      />

      {/* 사이드바가 열렸을 때 배경.
          앱 모드에서 사이드바 바깥을 눌러 닫을 수 있게 하는 자리다.
          웹 모드에서는 CSS 가 이것을 숨긴다.

          div 가 아니라 button 인 이유 — 키보드로도 닫을 수 있어야 한다.
          div 에 onClick 을 달면 마우스로만 눌린다 */}
      {navOpen && <button type="button" className="nav-backdrop" onClick={closeNav} />}

      <div className="shell__body">
        {/* 상단 메뉴 */}
        <TopBar user={user} notify={notify} onLogout={onLogout} onToggleNav={toggleNav} />

        {/* 페이지가 표시되는 부분.
            Outlet 은 "자식 Route 의 화면이 들어올 자리" 다.
            App.jsx 의 <Route index element={<HomePage />} /> 같은
            것들이 여기 그려진다 */}
        <main className="shell__main">
          <Outlet />
        </main>
      </div>

      {/* 모바일 하단 메뉴. 웹 모드에서는 CSS 가 숨긴다.
          조건부로 안 그리고 늘 그리는 이유 — 창 크기가 바뀌어도
          따라가야 하는데, 그것을 자바스크립트로 하려면 resize 를
          듣고 있어야 한다. CSS 가 알아서 한다 */}
      <BottomNav user={user} />
    </div>
  )
}
