import { createContext, useContext, useEffect, useState } from 'react'
import { getMe, login as loginApi } from '../api/auth'
import { setTokenExpiredHandler } from '../api/client'
import { getToken, removeToken, saveToken } from '../utils/token'

/**
 * 로그인 상태를 앱 전체에서 함께 쓰기 위한 곳.
 *
 * Context 가 왜 필요한가
 * ----------------------
 * 상단바도 "로그인한 사람 닉네임"이 필요하고, 게시글 상세도 필요하고,
 * ProtectedRoute 도 필요하다. 이걸 props 로 넘기면 중간에 있는 컴포넌트들이
 * 자기는 안 쓰면서 계속 전달만 해야 한다(prop drilling).
 * Context 는 그 중간 단계를 건너뛰고 필요한 곳에서 바로 꺼내 쓰게 해준다.
 *
 * 쓰는 법
 * -------
 *   const { user, isAuthenticated, login, logout } = useAuth()
 */

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // 로그인한 사람 정보. 로그인 안 했으면 null
  const [user, setUser] = useState(null)

  // 새로고침 직후 "토큰이 아직 살아있나" 서버에 물어보는 동안의 상태.
  //
  // 이게 없으면 ProtectedRoute 가 "아직 확인 중"을 "로그인 안 함"으로 오해해서,
  // 로그인 상태인데도 /login 으로 잠깐 튕겼다 돌아오는 깜빡임이 생긴다.
  const [restoring, setRestoring] = useState(true)

  /** 로그아웃. 토큰을 버리는 것으로 끝난다. */
  function logout() {
    removeToken()
    setUser(null)
    // 서버에는 알리지 않는다.
    // JWT 는 서버가 로그인 목록을 들고 있지 않아서(무상태),
    // 클라이언트가 토큰을 버리면 그걸로 로그아웃이 끝난다.
  }

  /** 로그인. 성공하면 토큰을 저장하고 사용자 정보를 화면에 반영한다. */
  async function login(email, password) {
    const result = await loginApi({ email, password })

    // 백엔드는 access_token 이라는 이름으로 준다(파이썬 쪽 이름 규칙).
    // 자바스크립트에서는 accessToken 으로 바꿔 쓴다.
    saveToken(result.access_token)
    setUser(result.user)

    return result.user
  }

  // --- 새로고침한 뒤 로그인 상태 되살리기 ---
  // useEffect 의 두 번째 인자가 빈 배열([])이면 "처음 한 번만 실행" 이라는 뜻이다.
  useEffect(() => {
    const savedToken = getToken()

    if (!savedToken) {
      // 저장된 토큰이 없다 = 로그인한 적 없다. 확인할 것도 없다.
      setRestoring(false)
      return
    }

    // 토큰이 있어도 만료됐을 수 있으니 서버에 물어본다.
    // 성공하면 로그인 상태로 복원, 실패하면 조용히 토큰을 버린다.
    getMe()
      .then((me) => setUser(me))
      .catch(() => removeToken())
      .finally(() => setRestoring(false))
  }, [])

  // --- 쓰던 도중 토큰이 만료되면 자동 로그아웃 ---
  //
  // api/client.js 가 401 을 받으면 여기 등록한 logout 을 불러준다.
  //
  // 의존성 배열이 [logout] 이 아니라 [] 인 이유:
  // logout 은 렌더할 때마다 새로 만들어지지만, 그 안에서 쓰는 removeToken(그냥 함수)과
  // setUser(리액트가 절대 안 바꿔주는 함수) 는 언제나 같은 것이다.
  // 그래서 첫 렌더의 logout 을 한 번만 등록해두면 계속 제대로 동작한다.
  // [logout] 으로 두면 렌더할 때마다 등록을 지웠다 다시 하게 되어 낭비다.
  useEffect(() => {
    setTokenExpiredHandler(logout)

    // 컴포넌트가 사라질 때 등록을 해제한다.
    // 안 하면 없어진 컴포넌트의 함수가 계속 불리게 된다.
    return () => setTokenExpiredHandler(null)
  }, [])

  // 아래 값들이 useAuth() 로 꺼내 쓸 수 있는 것들이다
  const value = {
    user,
    isAuthenticated: user !== null,
    restoring,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

/**
 * 로그인 정보를 꺼내 쓰는 함수.
 *
 * <AuthProvider> 바깥에서 부르면 null 이 나와서
 * "undefined 의 속성을 읽을 수 없다"는 알기 어려운 에러가 난다.
 * 그 전에 무엇이 잘못됐는지 알려준다.
 */
export function useAuth() {
  const context = useContext(AuthContext)

  if (context === null) {
    throw new Error('useAuth()는 <AuthProvider> 안에서만 쓸 수 있습니다.')
  }

  return context
}
