import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from '../components/common/Sidebar'
import TopBar from '../components/common/TopBar'
import BottomNav from '../components/common/BottomNav'
import '../styles/layouts/MainLayout.css'

/**
 * 공통 레이아웃.
 *
 * 웹 모드(1024px~) : [사이드바][상단바 + 본문]
 * 앱 모드(~1023px) : [상단바 + 본문] + 하단 탭
 */
export default function MainLayout() {
  const { pathname } = useLocation()

  // 페이지를 옮기면 맨 위에서 시작한다.
  // 네이티브 앱은 화면을 새로 밀어 올리므로 항상 위에서 시작하는데,
  // SPA는 스크롤 위치가 그대로 남아 목록 중간에서 상세가 열린 것처럼 보인다.
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return (
    <div className="shell">
      <Sidebar />

      <div className="shell__body">
        <TopBar />
        <main className="shell__main">
          <Outlet />
        </main>
      </div>

      <BottomNav />
    </div>
  )
}
