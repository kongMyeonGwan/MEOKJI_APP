import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from '../components/common/Sidebar'
import TopBar from '../components/common/TopBar'
import BottomNav from '../components/common/BottomNav'
import '../styles/layouts/MainLayout.css'

/**
 * 공통 레이아웃.
 *
 * 웹 모드(1024px~) : [사이드바][상단바 + 본문]
 * 앱 모드(~1023px) : [상단바 + 본문] + 하단 탭, 사이드바는 서랍으로 열린다
 *
 * 사이드바를 여닫는 상태를 여기서 들고 있는다. 여는 버튼(☰)은 상단바에 있고
 * 닫는 동작은 사이드바 안의 메뉴와 뒷배경에서 일어나서, 두 형제 컴포넌트가
 * 같은 값을 봐야 하기 때문이다.
 */

const WEB_MODE = '(min-width: 1024px)'

export default function MainLayout() {
  const { pathname } = useLocation()

  // 웹 모드는 펼친 채로, 앱 모드는 접힌 채로 시작한다.
  // 앱 모드에서 열려 있으면 첫 화면이 서랍에 가려진다.
  const [navOpen, setNavOpen] = useState(() => window.matchMedia(WEB_MODE).matches)

  // 창 크기가 경계를 넘어가면 그 모드의 기본값으로 되돌린다.
  // 이게 없으면 폰 크기로 줄였다 늘렸을 때 넓은 화면인데 사이드바가 사라져 있다.
  useEffect(() => {
    const mq = window.matchMedia(WEB_MODE)
    // 창 크기가 경계를 넘을 때마다 불린다.
    // e.matches 가 true 면 웹 모드(넓은 화면)라는 뜻이다.
    function sync(e) {
      setNavOpen(e.matches)
    }

    mq.addEventListener('change', sync)

    // 화면을 떠날 때 등록을 해제한다. 안 하면 계속 쌓인다.
    return () => mq.removeEventListener('change', sync)
  }, [])

  // 페이지를 옮기면 맨 위에서 시작한다.
  // 네이티브 앱은 화면을 새로 밀어 올리므로 항상 위에서 시작하는데,
  // SPA는 스크롤 위치가 그대로 남아 목록 중간에서 상세가 열린 것처럼 보인다.
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  // 앱 모드에서 메뉴를 눌러 화면이 바뀌면 서랍을 닫는다.
  // 웹 모드에서는 서랍이 아니라 고정 사이드바라 닫으면 안 된다.
  /**
   * 앱 모드에서 메뉴를 눌러 화면이 바뀌면 서랍을 닫는다.
   *
   * 웹 모드에서는 서랍이 아니라 항상 떠 있는 사이드바라서 닫으면 안 된다.
   */
  function closeDrawer() {
    const isWebMode = window.matchMedia(WEB_MODE).matches
    if (!isWebMode) {
      setNavOpen(false)
    }
  }

  return (
    <div className={`shell${navOpen ? ' is-nav-open' : ''}`}>
      <Sidebar onNavigate={closeDrawer} />

      {navOpen && (
        <button
          type="button"
          className="nav-backdrop"
          aria-label="메뉴 닫기"
          onClick={() => setNavOpen(false)}
        />
      )}

      <div className="shell__body">
        <TopBar onToggleNav={() => setNavOpen((prev) => !prev)} navOpen={navOpen} />
        <main className="shell__main">
          <Outlet />
        </main>
      </div>

      <BottomNav />
    </div>
  )
}
