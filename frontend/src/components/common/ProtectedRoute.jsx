import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Loading from './Loading'

// 로그인해야 들어갈 수 있는 화면을 감싸는 라우트.
// App.jsx 에서 <Route element={<ProtectedRoute />}> 안에 넣은 경로에만 적용된다.
export default function ProtectedRoute() {
  const { isAuthenticated, restoring } = useAuth()
  const location = useLocation()

  // 토큰 확인이 끝나기 전에 판단하면 안 된다.
  // 로그인 상태인데도 /login 이 잠깐 보였다 돌아오는 깜빡임이 생긴다.
  if (restoring) {
    return <Loading message="로그인 상태를 확인하는 중..." />
  }

  if (!isAuthenticated) {
    // 원래 가려던 곳을 같이 넘겨서 로그인한 다음 그 화면으로 보내준다.
    // replace 를 쓰면 뒤로가기를 눌러도 이 화면으로 안 돌아온다.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
