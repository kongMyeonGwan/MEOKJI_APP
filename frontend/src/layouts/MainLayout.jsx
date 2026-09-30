import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import Sidebar from '../components/common/Sidebar'
import TopBar from '../components/common/TopBar'
import BottomNav from '../components/common/BottomNav'
import '../styles/layouts/MainLayout.css'

// 모든 화면이 같이 쓰는 틀.
//   PC     : [사이드바][상단바 + 본문]
//   모바일 : [상단바 + 본문] + 하단 탭. 사이드바는 ☰ 를 누르면 서랍처럼 열린다.
//
// children 은 App.jsx 에서 <MainLayout> 안에 넣은 <Routes> 다.
export default function MainLayout({ children, user, onLogout, theme, onThemeChange, notify, onNotifyChange }) {
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
        <main className="shell__main">{children}</main>
      </div>

      <BottomNav user={user} />
    </div>
  )
}
