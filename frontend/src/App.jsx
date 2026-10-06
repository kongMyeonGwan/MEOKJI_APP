import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import { logout, refresh } from './api/auth'
import { removeToken, setToken } from './utils/token'
import { applyTheme, getSavedNotify, getSavedTheme, saveNotify } from './utils/settings'

// 화면들. pages/ 아래 담당자별 폴더로 나눠져 있다 (pages/README.md 참고)
import HomePage from './pages/home/HomePage'
import NotFoundPage from './pages/NotFoundPage'

import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import PasswordResetPage from './pages/auth/PasswordResetPage'

import RecommendPage from './pages/recommend/RecommendPage'
import BudgetPage from './pages/recommend/BudgetPage'
import SearchPage from './pages/recommend/SearchPage'
import RecipePage from './pages/recommend/RecipePage'

import GamePage from './pages/game/GamePage'
import WorldCupPage from './pages/game/WorldCupPage'
import BalancePage from './pages/game/BalancePage'
import RoulettePage from './pages/game/RoulettePage'

import ReviewListPage from './pages/community/recipeReview/ReviewListPage'
import PopularPage from './pages/community/recipeReview/PopularPage'
import ReviewDetailPage from './pages/community/recipeReview/ReviewDetailPage'
import ReviewWritePage from './pages/community/recipeReview/ReviewWritePage'

import NoticeListPage from './pages/community/notice/NoticeListPage'
import NoticeDetailPage from './pages/community/notice/NoticeDetailPage'
import NoticeWritePage from './pages/community/notice/NoticeWritePage'

import FreeListPage from './pages/community/free/FreeListPage'
import FreeDetailPage from './pages/community/free/FreeDetailPage'
import FreeWritePage from './pages/community/free/FreeWritePage'

import MyPage from './pages/mypage/MyPage'
import HistoryPage from './pages/mypage/HistoryPage'
import AiHistoryPage from './pages/mypage/AiHistoryPage'
import LikedPage from './pages/mypage/LikedPage'
import SettingsPage from './pages/mypage/SettingsPage'

// 어떤 주소에 어떤 화면을 보여줄지 정하는 곳.
//
// 여러 화면이 같이 쓰는 값은 여기서 useState 로 들고 있다가 props 로 내려준다.
//   user   : 로그인한 사람. 로그인 안 했으면 null
//   theme  : 'light' 또는 'dark'
//   notify : 알림 켜짐(true) / 꺼짐(false)
export default function App() {
  // 로그인 정보는 브라우저에 저장하지 않는다. 그래서 늘 "로그인 안 함" 으로 시작하고,
  // 아래 useEffect 가 서버에 물어서 되살린다 (utils/token.js 의 설명 참고).
  const [user, setUser] = useState(null)

  // 되살리는 동안인지. 이게 없으면 로그인한 사람이 F5 를 눌렀을 때
  // 아주 잠깐 로그인 화면이 번쩍 보였다가 제자리로 돌아온다.
  const [checking, setChecking] = useState(true)

  const [theme, setTheme] = useState(getSavedTheme())
  const [notify, setNotify] = useState(getSavedNotify())

  // ----- 로그인 · 로그아웃 -----

  // 로그인 성공 (LoginPage 가 부른다).
  //
  // Refresh Token 은 여기서 다루지 않는다. 서버가 HttpOnly 쿠키로 심어줬고
  // 자바스크립트는 그 쿠키를 읽을 수조차 없다. 우리가 챙기는 건 Access Token 뿐이다.
  const handleLogin = (token, loginUser) => {
    setToken(token) // 메모리에 토큰 보관 (api/client.js 가 꺼내 쓴다)
    setUser(loginUser) // 화면에 반영 (상단바에 닉네임이 뜬다)
  }

  // 로그아웃 (상단바, 설정, 마이페이지가 부른다)
  //
  // 서버에도 알려야 한다. 화면에서 토큰만 버리면 쿠키가 그대로 남아서,
  // 다음에 앱을 켤 때 아래 useEffect 가 로그인을 되살려버린다.
  const handleLogout = async () => {
    try {
      await logout()
    } catch (err) {
      // 서버가 꺼져 있어도 화면에서는 로그아웃시킨다. 여기서 막으면
      // 서버가 안 뜰 때 로그아웃조차 못 하게 된다.
    }
    removeToken()
    setUser(null)
  }

  // 내 정보가 바뀌었을 때 (MyPage 에서 닉네임을 바꾸면 부른다)
  const handleUserChange = (nextUser) => {
    setUser(nextUser)
  }

  // 테마 · 알림은 state 만 바꾼다. 화면에 적용하고 저장하는 건 아래 useEffect 가 한다.
  const handleThemeChange = (nextTheme) => {
    setTheme(nextTheme)
  }

  const handleNotifyChange = (nextNotify) => {
    setNotify(nextNotify)
  }

  // theme 이 바뀔 때마다 <html data-theme="..."> 를 바꾸고 브라우저에 저장한다
  // (React 가 그리는 화면 밖에 있는 <html> 을 건드리는 일이라 useEffect 에서 한다)
  useEffect(() => {
    applyTheme(theme)
  }, [theme])

  // notify 가 바뀔 때마다 브라우저에 저장한다
  useEffect(() => {
    saveNotify(notify)
  }, [notify])

  // 앱을 켤 때 한 번, 로그인 상태를 되살린다.
  //
  // Access Token 은 메모리에만 있어서 F5 한 번에 사라진다. 그래도 로그인이
  // 풀리지 않는 건 Refresh Token 쿠키 덕분이다.
  //
  //   앱 시작 → 메모리에 토큰 없음 → /api/auth/refresh (쿠키가 자동으로 실린다)
  //          → 서버가 쿠키를 검사 → 새 Access Token + 내 정보 → 로그인 복원
  //
  // 그래서 PC 를 껐다 켜도, 로그아웃하지 않았고 14일이 지나지 않았다면 그대로 로그인돼 있다.
  // 실패하면(로그인한 적 없음 · 로그아웃함 · 14일 지남) 그냥 손님으로 시작한다.
  useEffect(() => {
    const restore = async () => {
      try {
        const result = await refresh()
        setToken(result.access_token)
        setUser(result.user)
      } catch (err) {
        // 여기 오는 건 정상이다. 로그인한 적 없는 사람이 처음 들어온 경우가 대부분이다.
      }
      setChecking(false)
    }
    restore()
  }, [])

  // 로그인해야 볼 수 있는 화면을 감싼다.
  // 로그인했으면 그 화면을, 안 했으면 로그인 화면으로 보낸다(<Navigate>).
  const loginOnly = (page) => {
    if (user) {
      return page
    }
    return <Navigate to="/login" />
  }

  // 로그인 상태를 되살리는 중에는 아무것도 그리지 않는다.
  //
  // 이걸 빼면 로그인한 사람이 /settings 에서 F5 를 눌렀을 때, 아직 user 가 null
  // 이라 loginOnly 가 로그인 화면으로 보내버린다. 잠시 뒤 복원되지만 화면이
  // 한 번 튄 뒤라 되돌아오지 않는다.
  if (checking) {
    return null
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* 공통 틀. path 가 없고 element 만 있다 — 아래 화면들을 감싸는 역할이다.
            화면은 MainLayout 안의 <Outlet /> 자리에 들어간다 */}
        <Route
          element={
            <MainLayout
              user={user}
              onLogout={handleLogout}
              theme={theme}
              onThemeChange={handleThemeChange}
              notify={notify}
              onNotifyChange={handleNotifyChange}
            />
          }
        >
          {/* index 는 부모 주소 그대로(/) 일 때 보여줄 화면이다 */}
          <Route index element={<HomePage />} />

          {/* 로그인 · 회원가입 */}
          <Route path="login" element={<LoginPage onLogin={handleLogin} />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="password-reset" element={<PasswordResetPage />} />

          {/* 추천 · 레시피 */}
          <Route path="recommend" element={<RecommendPage />} />
          <Route path="budget" element={<BudgetPage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="recipe/:foodName" element={<RecipePage user={user} />} />

          {/* 메뉴 게임 */}
          <Route path="game" element={<GamePage />} />
          <Route path="game/worldcup" element={<WorldCupPage user={user} />} />
          <Route path="game/balance" element={<BalancePage user={user} />} />
          <Route path="roulette" element={<RoulettePage />} />

          {/* 커뮤니티. 보는 것은 로그인 없이도 된다 */}
          <Route path="popular" element={<PopularPage />} />
          {/* 레시피리뷰 · 공지는 퍼블리싱만 돼 있다. 로그인한 사람(user)을 아직 쓰지 않아
              props 를 넘기지 않는다 — 기능 담당이 user={user} 를 붙이는 자리다 */}
          <Route path="community" element={<ReviewListPage />} />
          <Route path="community/:reviewId" element={<ReviewDetailPage />} />
          <Route path="community/write" element={loginOnly(<ReviewWritePage />)} />

          {/* 공지사항. 보기는 누구나, 작성 · 수정 · 삭제는 관리자만 (지금은 퍼블리싱만) */}
          <Route path="notice" element={<NoticeListPage />} />
          <Route path="notice/write" element={<NoticeWritePage />} />
          <Route path="notice/:noticeId" element={<NoticeDetailPage />} />

          {/* 자유게시판. 후기와 달리 아무 주제나 쓸 수 있다. 보는 것은 로그인 없이도 된다 */}
          <Route path="free" element={<FreeListPage user={user} />} />
          <Route path="free/write" element={loginOnly(<FreeWritePage />)} />
          <Route path="free/:postId" element={<FreeDetailPage user={user} />} />

          {/* 마이페이지 */}
          <Route
            path="settings"
            element={
              <SettingsPage
                user={user}
                onLogout={handleLogout}
                theme={theme}
                onThemeChange={handleThemeChange}
                notify={notify}
                onNotifyChange={handleNotifyChange}
              />
            }
          />
          <Route
            path="mypage"
            element={loginOnly(<MyPage user={user} onLogout={handleLogout} onUserChange={handleUserChange} />)}
          />
          <Route path="history" element={loginOnly(<HistoryPage />)} />
          <Route path="ai-history" element={loginOnly(<AiHistoryPage />)} />
          <Route path="liked" element={loginOnly(<LikedPage />)} />

          {/* 위에 없는 주소 */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
