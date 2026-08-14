import { BrowserRouter, Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import CommunityPage from './pages/CommunityPage'
import HistoryPage from './pages/HistoryPage'
import MyPage from './pages/MyPage'
import NotFoundPage from './pages/NotFoundPage'

/**
 * 라우팅 정의.
 * 로그인이 필요한 경로(history, mypage)는 STEP 6에서 ProtectedRoute로 감싼다.
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="community" element={<CommunityPage />} />
          <Route path="history" element={<HistoryPage />} />
          <Route path="mypage" element={<MyPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
