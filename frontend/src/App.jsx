import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AiLogProvider } from './contexts/AiLogContext'
import { AuthProvider } from './contexts/AuthContext'
import { CatalogProvider } from './contexts/CatalogContext'
import { LikesProvider } from './contexts/LikesContext'
import { PrefsProvider } from './contexts/PrefsContext'
import { ProfileProvider } from './contexts/ProfileContext'
import ProtectedRoute from './components/common/ProtectedRoute'
import MainLayout from './layouts/MainLayout'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import PasswordResetPage from './pages/PasswordResetPage'
import RegisterPage from './pages/RegisterPage'
import RecommendPage from './pages/RecommendPage'
import BudgetPage from './pages/BudgetPage'
import SearchPage from './pages/SearchPage'
import FridgePage from './pages/FridgePage'
import GamePage from './pages/GamePage'
import WorldCupPage from './pages/WorldCupPage'
import BalancePage from './pages/BalancePage'
import RoulettePage from './pages/RoulettePage'
import LikedPage from './pages/LikedPage'
import PopularPage from './pages/PopularPage'
import RecipePage from './pages/RecipePage'
import CommunityPage from './pages/CommunityPage'
import PostDetailPage from './pages/PostDetailPage'
import PostWritePage from './pages/PostWritePage'
import HistoryPage from './pages/HistoryPage'
import AiHistoryPage from './pages/AiHistoryPage'
import SettingsPage from './pages/SettingsPage'
import MyPage from './pages/MyPage'
import NotFoundPage from './pages/NotFoundPage'

/**
 * 라우팅 정의.
 *
 * 사이드바/하단탭의 메뉴가 여기 경로와 1:1로 대응한다.
 * community 목록·상세와 인기/찜 목록은 Guest도 볼 수 있어야 하므로
 * ProtectedRoute로 감싸지 않는다(STEP 20).
 *
 * Provider 순서
 * -------------
 *   PrefsProvider    테마·알림. 로그인과 무관하고 가장 바깥에서 <html>을 칠한다
 *   CatalogProvider  카테고리·게임 같은 고정 목록. 서버에서 한 번만 받아온다
 *   AuthProvider     로그인 상태
 *   ProfileProvider  요리 레벨·알림. useAuth를 쓰므로 반드시 Auth 안쪽이다
 *   LikesProvider    찜 목록. 서버(RECIPE_LIKES)에서 받아온다. useAuth 로 로그인
 *                    상태를 보므로 반드시 Auth 안쪽이다
 *   AiLogProvider    AI 추천 기록. 브라우저에만 저장한다
 */
export default function App() {
  return (
    <BrowserRouter>
      <PrefsProvider>
        <CatalogProvider>
          <AuthProvider>
            <ProfileProvider>
              <LikesProvider>
                <AiLogProvider>
                  <Routes>
                    <Route element={<MainLayout />}>
                      <Route index element={<HomePage />} />

                      {/* 인증 */}
                      <Route path="login" element={<LoginPage />} />
                      <Route path="register" element={<RegisterPage />} />
                      <Route path="password-reset" element={<PasswordResetPage />} />

                      {/* 추천 · 레시피 */}
                      <Route path="recommend" element={<RecommendPage />} />
                      <Route path="budget" element={<BudgetPage />} />
                      <Route path="search" element={<SearchPage />} />
                      <Route path="fridge" element={<FridgePage />} />
                      <Route path="recipe/:foodName" element={<RecipePage />} />

                      {/* 메뉴 게임 */}
                      <Route path="game" element={<GamePage />} />
                      <Route path="game/worldcup" element={<WorldCupPage />} />
                      <Route path="game/balance" element={<BalancePage />} />
                      <Route path="roulette" element={<RoulettePage />} />

                      {/* 커뮤니티 — 조회는 Guest 허용 */}
                      <Route path="popular" element={<PopularPage />} />
                      <Route path="community" element={<CommunityPage />} />
                      <Route path="community/:postId" element={<PostDetailPage />} />

                      {/* 설정은 이 브라우저에 저장되는 값이라 Guest도 쓸 수 있다 */}
                      <Route path="settings" element={<SettingsPage />} />

                      {/* 로그인 필수 */}
                      <Route element={<ProtectedRoute />}>
                        {/* 찜은 서버(RECIPE_LIKES)에 회원별로 저장된다.
                            Guest 에게는 보여줄 것이 없으므로 목록도 로그인 필수다 */}
                        <Route path="liked" element={<LikedPage />} />
                        <Route path="community/write" element={<PostWritePage />} />
                        <Route path="history" element={<HistoryPage />} />
                        <Route path="ai-history" element={<AiHistoryPage />} />
                        <Route path="mypage" element={<MyPage />} />
                      </Route>

                      <Route path="*" element={<NotFoundPage />} />
                    </Route>
                  </Routes>
                </AiLogProvider>
              </LikesProvider>
            </ProfileProvider>
          </AuthProvider>
        </CatalogProvider>
      </PrefsProvider>
    </BrowserRouter>
  )
}
