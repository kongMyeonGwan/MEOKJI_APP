import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import ProtectedRoute from './components/common/ProtectedRoute'
import MainLayout from './layouts/MainLayout'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import PasswordResetPage from './pages/PasswordResetPage'
import RegisterPage from './pages/RegisterPage'
import RecommendPage from './pages/RecommendPage'
import BudgetPage from './pages/BudgetPage'
import LikedPage from './pages/LikedPage'
import RecipePage from './pages/RecipePage'
import CommunityPage from './pages/CommunityPage'
import PostDetailPage from './pages/PostDetailPage'
import PostWritePage from './pages/PostWritePage'
import HistoryPage from './pages/HistoryPage'
import MyPage from './pages/MyPage'
import NotFoundPage from './pages/NotFoundPage'

/**
 * 라우팅 정의.
 *
 * 사이드바/하단탭의 메뉴가 여기 경로와 1:1로 대응한다.
 * community 목록·상세와 좋아요 목록은 Guest도 볼 수 있어야 하므로
 * ProtectedRoute로 감싸지 않는다(STEP 20).
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
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
            <Route path="recipe/:foodName" element={<RecipePage />} />

            {/* 커뮤니티 — 조회는 Guest 허용 */}
            <Route path="liked" element={<LikedPage />} />
            <Route path="community" element={<CommunityPage />} />
            <Route path="community/:postId" element={<PostDetailPage />} />

            {/* 로그인 필수 */}
            <Route element={<ProtectedRoute />}>
              <Route path="community/write" element={<PostWritePage />} />
              <Route path="history" element={<HistoryPage />} />
              <Route path="mypage" element={<MyPage />} />
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
