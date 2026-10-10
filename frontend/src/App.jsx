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
//
// 왜 맨 위에서 들고 있나
//
// 이 셋은 여러 화면이 같이 본다. 로그인 정보는 상단바 · 사이드바 ·
// 마이페이지가 같이 쓰고, 로그인하면 그 셋이 한꺼번에 바뀌어야 한다.
// 화면마다 따로 들고 있으면 한 화면에서 로그아웃해도 다른 곳에는
// 닉네임이 남는다.
//
// 그래서 값은 여기 두고 props 로 내려주며, 바꾸는 함수도 같이 내려준다.
// 아래쪽 화면이 handleLogin 을 부르면 여기 state 가 바뀌고,
// 그 값을 받는 화면들이 한꺼번에 다시 그려진다.
//
// 내려주는 깊이가 두세 단계라 아직 손으로 넘길 만하다. 더 깊어지면
// 넘기기만 하는 컴포넌트가 늘어나므로 그때 다른 방법을 생각해야 한다.
export default function App() {
  // 브라우저에 저장해둔 값으로 시작한다. 그래서 새로고침해도 로그인이 유지된다.
  //
  // useState(null) 로 두고 useEffect 에서 채우면, 화면이 한 번
  // "로그인 안 한 모양" 으로 그려졌다가 바뀐다. 상단바의 로그인
  // 버튼이 번쩍하고 사라지는 것이 눈에 띈다
  const [user, setUser] = useState(getSavedUser())
  const [theme, setTheme] = useState(getSavedTheme())
  const [notify, setNotify] = useState(getSavedNotify())

  // ----- 로그인 · 로그아웃 -----
  // 로그인 정보를 브라우저에 저장하고 지우는 건 전부 여기서 한다.
  // (저장하는 함수들은 utils/token.js 에 있다)

  // 로그인 성공 (LoginPage 가 부른다).
  // keep 은 '로그인 상태 유지' 체크 여부다. true 면 브라우저를 껐다 켜도 남고,
  // false 면 탭을 닫을 때 사라진다 (utils/token.js 참고).
  // 세 가지를 같이 하는 이유 — 하나라도 빠뜨리면 상태가 어긋난다.
  // 토큰만 저장하면 새로고침할 때 닉네임이 없고, state 만 바꾸면
  // 새로고침하는 순간 로그인이 풀린다
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
  //
  // keep 을 안 넘긴다. saveUser 가 "지금 토큰이 들어 있는 쪽" 을 보고
  // 알아서 맞춘다 - 여기서 정하면 로그인할 때 고른 설정이 뒤집힌다
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
  //
  // 왜 필요한가 — 브라우저에 저장된 정보는 옛 값일 수 있다. 다른
  // 기기에서 닉네임을 바꿨거나, 그 사이에 토큰이 만료됐을 수 있다.
  //
  // 토큰이 만료된 것을 여기서 알아내지 않으면, 사용자는 로그인된
  // 화면을 보면서 버튼을 누를 때마다 401 을 받게 된다.
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
    // 의존성 배열이 비어 있다 - 처음 한 번만 실행한다.
    //
    // [user] 로 두면 안 된다. 안에서 handleUserChange 가 user 를
    // 바꾸므로 그때 다시 실행되고, 끝없이 서버를 부르게 된다
  }, [])

  // 로그인해야 볼 수 있는 화면을 감싼다.
  // 로그인했으면 그 화면을, 안 했으면 로그인 화면으로 보낸다(<Navigate>).
  //
  // 화면 안에서 각자 확인하게 두면, 새 화면을 만들 때 그 한 줄을
  // 빠뜨릴 수 있다. 빠뜨려도 에러가 안 나고 그냥 열린다.
  //
  // 이것이 보안은 아니다. 주소를 직접 쳐도 막히지만, 그건 화면을
  // 안 보여주는 것일 뿐이다 - 데이터는 서버가 토큰을 보고 막는다
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
          {/* index 는 부모 주소 그대로(/) 일 때 보여줄 화면이다.
            path="" 로 적는 것과 같지만, index 쪽이 뜻이 분명하다 */}
          <Route index element={<HomePage />} />

          {/* 로그인 · 회원가입 */}
          <Route path="login" element={<LoginPage onLogin={handleLogin} />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="password-reset" element={<PasswordResetPage />} />

          {/* 추천 · 레시피. 로그인 없이도 볼 수 있다 -
              첫 화면에서 바로 추천을 받아볼 수 있어야 한다 */}
          <Route path="recommend" element={<RecommendPage />} />
          <Route path="budget" element={<BudgetPage />} />
          <Route path="search" element={<SearchPage />} />
          {/* :foodName 은 주소에서 받아오는 값이다. /recipe/김치찌개 로
              들어오면 그 화면이 useParams 로 '김치찌개' 를 꺼낸다 */}
          <Route path="recipe/:foodName" element={<RecipePage user={user} />} />

          {/* 메뉴 게임 */}
          <Route path="game" element={<GamePage />} />
          <Route path="game/worldcup" element={<WorldCupPage user={user} />} />
          <Route path="game/balance" element={<BalancePage user={user} />} />
          {/* 룰렛만 /game 아래가 아니다. 사이드바에서 바로 들어가는
              자리가 있어서 짧은 주소를 따로 두었다 */}
          <Route path="roulette" element={<RoulettePage />} />

          {/* 커뮤니티. 보는 것은 로그인 없이도 된다 */}
          <Route path="popular" element={<PopularPage />} />
          {/* 레시피리뷰 · 공지는 퍼블리싱만 돼 있다. 로그인한 사람(user)을 아직 쓰지 않아
              props 를 넘기지 않는다 — 기능 담당이 user={user} 를 붙이는 자리다 */}
          <Route path="reviews" element={<ReviewListPage />} />
          <Route path="reviews/:reviewId" element={<ReviewDetailPage />} />
          <Route path="reviews/write" element={loginOnly(<ReviewWritePage />)} />

          {/* /community 와 /notice 가 같은 화면을 가리킨다. 사이드바의
              "커뮤니티" 를 눌렀을 때 들어갈 첫 화면이 공지라서다 */}
          <Route path="community" element={<NoticeListPage />} />

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
          {/* AI 추천 기록은 주석 처리돼 있다. 화면(AiHistoryPage)은 만들어
              두었지만 아직 열지 않았다 - 상단바 프로필 메뉴에는 링크가
              남아 있어서, 그것을 누르면 NotFoundPage 로 떨어진다 */}
          {/* <Route path="ai-history" element={loginOnly(<AiHistoryPage />)} /> */}
          <Route path="liked" element={loginOnly(<LikedPage />)} />

          {/* 위에 없는 주소. * 는 "나머지 전부" 다.
              맨 아래 두어야 한다 - 위에 두면 모든 주소가 여기 걸린다 */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
