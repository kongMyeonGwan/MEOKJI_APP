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

// 어떤 주소에 어떤 화면을 보여줄지 정하는 곳.
// 사이드바랑 하단 탭의 메뉴가 여기 주소랑 하나씩 짝이 맞는다.
//
// Provider 를 여러 겹 감싸는데 순서가 중요하다.
//   PrefsProvider    테마·알림. 로그인이랑 상관없어서 제일 바깥에 둔다.
//   CatalogProvider  카테고리·게임 같은 고정 목록.
//   AuthProvider     로그인 상태.
//   ProfileProvider  내 정보·알림. useAuth 를 써서 Auth 안쪽이어야 한다.
//   LikesProvider    찜 목록. 이것도 useAuth 를 써서 Auth 안쪽이다.
//   AiLogProvider    AI 추천 기록. 브라우저에만 저장한다.
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
                      <Route path="recipe/:foodName" element={<RecipePage />} />

                      {/* 메뉴 게임 */}
                      <Route path="game" element={<GamePage />} />
                      <Route path="game/worldcup" element={<WorldCupPage />} />
                      <Route path="game/balance" element={<BalancePage />} />
                      <Route path="roulette" element={<RoulettePage />} />

                      {/* 커뮤니티. 보는 것은 로그인 없이도 된다 */}
                      <Route path="popular" element={<PopularPage />} />
                      <Route path="community" element={<CommunityPage />} />
                      <Route path="community/:postId" element={<PostDetailPage />} />

                      {/* 설정은 브라우저에 저장되는 값이라 로그인 없이도 쓸 수 있다 */}
                      <Route path="settings" element={<SettingsPage />} />

                      {/* 로그인 필수 */}
                      <Route element={<ProtectedRoute />}>
                        {/* 찜은 서버에 회원별로 저장돼서 목록도 로그인이 필요하다 */}
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
