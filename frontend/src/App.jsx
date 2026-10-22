import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import { getMe } from './api/auth'
import { getSavedUser, removeToken, removeUser, saveToken, saveUser } from './utils/token'
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
  // 브라우저에 저장해둔 값으로 시작한다. 그래서 새로고침해도 로그인이 유지된다.
  const [user, setUser] = useState(getSavedUser())
  const [theme, setTheme] = useState(getSavedTheme())
  const [notify, setNotify] = useState(getSavedNotify())

  // ----- 로그인 · 로그아웃 -----
  // 로그인 정보를 브라우저에 저장하고 지우는 건 전부 여기서 한다.
  // (저장하는 함수들은 utils/token.js 에 있다)

  // 로그인 성공 (LoginPage 가 부른다).
  // keep 은 '로그인 상태 유지' 체크 여부다. true 면 브라우저를 껐다 켜도 남고,
  // false 면 탭을 닫을 때 사라진다 (utils/token.js 참고).
  const handleLogin = (token, loginUser, keep) => {
    saveToken(token, keep) // 브라우저에 토큰 저장
    saveUser(loginUser, keep) // 브라우저에 내 정보 저장
    setUser(loginUser) // 화면에 반영 (상단바에 닉네임이 뜬다)
  }

  // 로그아웃 (상단바, 설정, 마이페이지가 부른다)
  const handleLogout = () => {
    removeToken()
    removeUser()
    setUser(null)
  }

  // 내 정보가 바뀌었을 때 (MyPage 에서 닉네임을 바꾸면 부른다)
  const handleUserChange = (nextUser) => {
    saveUser(nextUser)
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

  // 앱을 처음 켤 때 한 번, 서버에서 내 정보를 다시 받아 최신으로 맞춘다.
  useEffect(() => {
    if (user === null) {
      return // 로그인 안 했으면 확인할 게 없다
    }

    const checkLogin = async () => {
      try {
        const me = await getMe()
        handleUserChange(me)
      } catch (err) {
        // 401 이면 토큰이 만료된 것이다 (토큰은 30분 뒤 만료된다) → 로그아웃 상태로
        if (err.response && err.response.status === 401) {
          handleLogout()
        }
        // 서버가 꺼져 있는 경우는 저장해둔 정보를 그대로 쓴다
      }
    }
    checkLogin()
  }, [])

  // 로그인해야 볼 수 있는 화면을 감싼다.
  // 로그인했으면 그 화면을, 안 했으면 로그인 화면으로 보낸다(<Navigate>).
  const loginOnly = (page) => {
    if (user) {
      return page
    }
    return <Navigate to="/login" />
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
          {/* 사이드바 '커뮤니티' 는 공지사항 목록으로 보낸다 */}
          <Route path="community" element={<Navigate to="/notice" replace />} />

          <Route path="popular" element={<PopularPage />} />
          {/* 레시피리뷰 · 공지는 퍼블리싱만 돼 있다. 로그인한 사람(user)을 아직 쓰지 않아
              props 를 넘기지 않는다 — 기능 담당이 user={user} 를 붙이는 자리다 */}
          <Route path="reviews" element={<ReviewListPage />} />
          <Route path="reviews/:reviewId" element={<ReviewDetailPage user={user} />} />
          <Route path="reviews/write" element={loginOnly(<ReviewWritePage />)} />

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
