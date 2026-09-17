import { createContext, useContext, useEffect, useState } from 'react'
import { getMe, login as loginApi } from '../api/auth'
import { setTokenExpiredHandler } from '../api/client'
import { getToken, removeToken, saveToken } from '../utils/token'

// 로그인 상태를 앱 전체에서 같이 쓰기 위한 Context.
// 상단바, 게시글 상세, ProtectedRoute 가 전부 로그인 정보를 필요로 하는데
// props 로 넘기면 중간 컴포넌트들이 쓰지도 않으면서 계속 전달만 해야 한다.
//
// 쓰는 법:
//   const { user, isAuthenticated, login, logout } = useAuth()

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // 로그인한 사람 정보. 로그인 안 했으면 null
  const [user, setUser] = useState(null)

  // 새로고침한 직후 토큰이 살아있는지 서버에 물어보는 동안의 상태.
  // 이게 없으면 ProtectedRoute 가 "확인 중" 을 "로그인 안 함" 으로 보고
  // 로그인 상태인데도 /login 으로 잠깐 튕긴다.
  const [restoring, setRestoring] = useState(true)

  // 로그아웃. 토큰을 버리면 끝난다.
  // 서버에는 안 알린다. JWT 는 서버가 로그인 목록을 안 들고 있어서
  // 브라우저가 토큰을 버리는 걸로 로그아웃이 된다.
  function logout() {
    removeToken()
    setUser(null)
  }

  // 로그인. 성공하면 토큰을 저장하고 화면에 사용자 정보를 반영한다.
  async function login(email, password) {
    const result = await loginApi({ email: email, password: password })

    // 백엔드는 access_token 이라는 이름으로 준다 (파이썬 이름 규칙)
    saveToken(result.access_token)
    setUser(result.user)

    return result.user
  }

  // 새로고침한 뒤 로그인 상태 되살리기.
  // useEffect 의 두 번째 인자가 [] 면 처음 한 번만 실행된다는 뜻이다.
  useEffect(() => {
    const savedToken = getToken()

    if (!savedToken) {
      // 저장된 토큰이 없으면 로그인한 적이 없다는 뜻이라 확인할 게 없다
      setRestoring(false)
      return
    }

    // 토큰이 있어도 만료됐을 수 있으니 서버에 물어본다.
    // 성공하면 로그인 상태로 되돌리고, 실패하면 그냥 토큰을 버린다.
    getMe()
      .then(function (me) {
        setUser(me)
      })
      .catch(function () {
        removeToken()
      })
      .finally(function () {
        setRestoring(false)
      })
  }, [])

  // 쓰다가 토큰이 만료되면 자동으로 로그아웃시킨다.
  // api/client.js 가 401 을 받으면 여기서 등록한 함수를 불러준다.
  useEffect(() => {
    setTokenExpiredHandler(logout)

    // 컴포넌트가 사라질 때 등록을 풀어준다
    return function () {
      setTokenExpiredHandler(null)
    }
  }, [])

  // 아래 값들을 useAuth() 로 꺼내 쓸 수 있다
  const value = {
    user: user,
    isAuthenticated: user !== null,
    restoring: restoring,
    login: login,
    logout: logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// 로그인 정보를 꺼내 쓰는 함수.
// <AuthProvider> 바깥에서 부르면 무슨 에러인지 알기 어려워서 미리 알려준다.
export function useAuth() {
  const context = useContext(AuthContext)

  if (context === null) {
    throw new Error('useAuth()는 <AuthProvider> 안에서만 쓸 수 있습니다.')
  }

  return context
}
