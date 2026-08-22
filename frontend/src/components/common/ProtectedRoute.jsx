import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Loading from './Loading'

/**
 * 로그인이 필요한 경로를 감싸는 라우트.
 * App.jsx에서 <Route element={<ProtectedRoute />}> 안에 넣은 경로에만 적용된다.
 */
export default function ProtectedRoute() {
  const { isAuthenticated, restoring } = useAuth()
  const location = useLocation()

  // 토큰 검증이 끝나기 전에 판단하면 안 된다.
  // 로그인 상태인데도 잠깐 /login이 보였다가 돌아오는 깜빡임이 생긴다.
  if (restoring) return <Loading message="로그인 상태를 확인하는 중..." />

  if (!isAuthenticated) {
    // 원래 가려던 곳을 넘겨줘서, 로그인 후 그 페이지로 돌려보낸다.
    // replace를 쓰면 뒤로가기 눌렀을 때 보호된 페이지로 되돌아가지 않는다.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
