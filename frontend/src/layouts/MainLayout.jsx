import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'

import Sidebar from './components/Sidebar'
import TopBar from './components/TopBar'
import BottomNav from './components/BottomNav'

export default function MainLayout({ user, onLogout, theme, onThemeChange, notify, onNotifyChange }) {
  const location = useLocation()

  // 사이드바 열기 / 닫기. 웹 모드(1024px 이상)는 연 채로 시작한다
  const [navOpen, setNavOpen] = useState(window.innerWidth >= 1024)

  // 화면을 옮기면 스크롤을 맨 위로 올린다.
  // 목록을 한참 내려보다 글을 누르면 상세가 중간부터 보인다.
  useEffect(() => {
    window.scrollTo(0, 0)
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

      {/* 사이드바가 열렸을 때 배경 */}
      {navOpen && <button type="button" className="nav-backdrop" onClick={closeNav} />}

      <div className="shell__body">
        {/* 상단 메뉴 */}
        <TopBar user={user} notify={notify} onLogout={onLogout} onToggleNav={toggleNav} />

        {/* 페이지가 표시되는 부분 */}
        <main className="shell__main">
          <Outlet />
        </main>
      </div>

      {/* 모바일 하단 메뉴 */}
      <BottomNav user={user} />
    </div>
  )
}
