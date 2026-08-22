import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { getMe, login as loginApi } from '../api/auth'
import { setAuthToken, setUnauthorizedHandler } from '../api/client'

/**
 * 로그인 상태를 앱 전체에서 공유한다.
 *
 * 토큰을 localStorage에 두는 이유
 * ------------------------------
 * 새로고침이나 탭을 다시 열어도 로그인이 유지되어야 하는데,
 * 서버가 Refresh Token을 발급하지 않기로 했기 때문에 클라이언트가 들고 있어야 한다.
 * localStorage는 XSS에 노출된다는 약점이 있다(스크립트가 읽어갈 수 있다).
 * 더 안전한 방법은 HttpOnly 쿠키지만 CSRF 대응이 따로 필요해 MVP 범위를 넘는다.
 * 대신 토큰 수명을 30분으로 짧게 잡았다.
 */

const TOKEN_KEY = 'meokji.accessToken'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  // 새로고침 직후 토큰 검증이 끝날 때까지의 과도기.
  // 이걸 두지 않으면 ProtectedRoute가 "아직 모름"을 "비로그인"으로 오해해서
  // 로그인 상태인데도 /login으로 튕겨버린다.
  const [restoring, setRestoring] = useState(true)

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setAuthToken(null)
    setUser(null)
    // 서버에는 알리지 않는다. JWT는 서버가 상태를 갖지 않으므로
    // 토큰을 버리는 것으로 로그아웃이 끝난다.
  }, [])

  // --- 새로고침 후 상태 복원 ---
  useEffect(() => {
    const saved = localStorage.getItem(TOKEN_KEY)
    if (!saved) {
      setRestoring(false)
      return
    }

    // 저장된 토큰이 만료됐을 수도 있으므로 서버에 물어봐서 확인한다.
    setAuthToken(saved)
    let cancelled = false // StrictMode의 개발용 이중 실행 대비

    getMe()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        // 만료·위조 토큰. 조용히 버리고 비로그인 상태로 시작한다.
        localStorage.removeItem(TOKEN_KEY)
        setAuthToken(null)
      })
      .finally(() => {
        if (!cancelled) setRestoring(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  // --- 사용 중 토큰이 만료된 경우 자동 로그아웃 ---
  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  const login = useCallback(async (email, password) => {
    const { access_token: accessToken, user: me } = await loginApi({ email, password })
    localStorage.setItem(TOKEN_KEY, accessToken)
    setAuthToken(accessToken)
    setUser(me)
    return me
  }, [])

  const value = useMemo(
    () => ({ user, isAuthenticated: user !== null, restoring, login, logout }),
    [user, restoring, login, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (ctx === null) {
    throw new Error('useAuth()는 <AuthProvider> 안에서만 쓸 수 있습니다.')
  }
  return ctx
}
