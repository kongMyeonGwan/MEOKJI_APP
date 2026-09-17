import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from '../components/common/Sidebar'
import TopBar from '../components/common/TopBar'
import BottomNav from '../components/common/BottomNav'
import '../styles/layouts/MainLayout.css'

// 모든 화면이 같이 쓰는 레이아웃.
//
// PC(1024px~)  : [사이드바][상단바 + 본문]
// 모바일(~1023px) : [상단바 + 본문] + 하단 탭, 사이드바는 서랍으로 열린다
//
// 사이드바를 여닫는 상태를 여기서 들고 있다.
// 여는 버튼(☰)은 상단바에 있고 닫는 건 사이드바 안이랑 뒷배경에서 일어나서
// 두 컴포넌트가 같은 값을 봐야 하기 때문이다.

const WEB_MODE = '(min-width: 1024px)'

export default function MainLayout() {
  const { pathname } = useLocation()

  // PC 는 펼친 채로, 모바일은 접힌 채로 시작한다.
  // 모바일에서 열려 있으면 첫 화면이 서랍에 가려진다.
  const [navOpen, setNavOpen] = useState(function () {
    return window.matchMedia(WEB_MODE).matches
  })

  // 창 크기가 경계를 넘으면 그 모드의 기본값으로 되돌린다.
  // 이게 없으면 폰 크기로 줄였다 늘렸을 때 넓은 화면인데 사이드바가 사라져 있다.
  useEffect(() => {
    const mq = window.matchMedia(WEB_MODE)

    // 창 크기가 경계를 넘을 때마다 불린다.
    // e.matches 가 true 면 PC 모드(넓은 화면) 라는 뜻이다.
    function sync(e) {
      setNavOpen(e.matches)
    }

    mq.addEventListener('change', sync)

    // 화면을 떠날 때 등록을 풀어준다. 안 하면 계속 쌓인다.
    return function () {
      mq.removeEventListener('change', sync)
    }
  }, [])

  // 화면을 옮기면 맨 위에서 시작한다.
  // 안 하면 스크롤 위치가 그대로 남아서 목록 중간에서 상세가 열린 것처럼 보인다.
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  // 모바일에서 메뉴를 눌러 화면이 바뀌면 서랍을 닫는다.
  // PC 에서는 서랍이 아니라 항상 떠 있는 사이드바라서 닫으면 안 된다.
  function closeDrawer() {
    const isWebMode = window.matchMedia(WEB_MODE).matches
    if (!isWebMode) {
      setNavOpen(false)
    }
  }

  let shellClass = 'shell'
  if (navOpen) {
    shellClass = 'shell is-nav-open'
  }

  return (
    <div className={shellClass}>
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
        <TopBar onToggleNav={() => setNavOpen(!navOpen)} navOpen={navOpen} />
        <main className="shell__main">
          <Outlet />
        </main>
      </div>

      <BottomNav />
    </div>
  )
}
