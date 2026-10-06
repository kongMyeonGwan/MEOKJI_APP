import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import TopBar from './components/TopBar'
import BottomNav from './components/BottomNav'

// 모든 화면이 같이 쓰는 틀.
//   PC     : [사이드바][상단바 + 본문]
//   모바일 : [상단바 + 본문] + 하단 탭. 사이드바는 ☰ 를 누르면 서랍처럼 열린다.
//
// 화면이 바뀌어도 사이드바 · 상단바 · 하단 탭은 그대로 있고 본문만 갈린다.
// 그 '본문 자리' 가 아래 <Outlet /> 이다.
//
// App.jsx 에서 이 틀을 이렇게 씌운다.
//
//   <Route element={<MainLayout ... />}>   {/* path 가 없다 — 감싸는 역할만 */}
//     <Route index element={<HomePage />} />
//     <Route path="free" element={<FreeListPage />} />
//   </Route>
//
// 주소가 /free 면 React Router 가 FreeListPage 를 <Outlet /> 자리에 끼워 넣는다.
export default function MainLayout({ user, onLogout, theme, onThemeChange, notify, onNotifyChange }) {
  const location = useLocation()

  // 사이드바가 열려 있는지. PC(너비 1024px 이상)는 열린 채로 시작한다.
  const [navOpen, setNavOpen] = useState(window.innerWidth >= 1024)

  // 화면을 옮기면 스크롤을 맨 위로 올린다
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [location.pathname])

  // 모바일에서 메뉴를 누르면 서랍을 닫는다 (PC 는 그대로 둔다)
  const closeDrawer = () => {
    if (window.innerWidth < 1024) {
      setNavOpen(false)
    }
  }

  return (
    <div className={navOpen ? 'shell is-nav-open' : 'shell'}>
      <Sidebar
        onNavigate={closeDrawer}
        user={user}
        theme={theme}
        onThemeChange={onThemeChange}
        notify={notify}
        onNotifyChange={onNotifyChange}
      />

      {/* 모바일에서 서랍이 열렸을 때 뒤를 어둡게 덮는 판. 누르면 닫힌다 */}
      {navOpen && <button type="button" className="nav-backdrop" onClick={() => setNavOpen(false)} />}

      <div className="shell__body">
        <TopBar onToggleNav={() => setNavOpen(!navOpen)} user={user} onLogout={onLogout} notify={notify} />

        {/* 주소에 맞는 화면이 여기에 들어온다 */}
        <main className="shell__main">
          <Outlet />
        </main>
      </div>

      <BottomNav user={user} />
    </div>
  )
}
