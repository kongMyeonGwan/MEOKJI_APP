import { createContext, useContext, useEffect, useState } from 'react'

// 테마(밝게/어둡게)랑 알림 설정을 앱 전체에서 같이 쓰기 위한 Context.
// 로그인이랑 상관없이 이 브라우저에만 저장되는 값이라 localStorage 를 쓴다.
//
// 테마는 <html data-theme="dark"> 한 곳만 바꾼다.
// 그러면 index.css 에 만들어둔 CSS 변수가 통째로 바뀌어서 화면 전체가 따라온다.
//
// 쓰는 법:
//   const { theme, setTheme, notify, setNotify } = usePrefs()

const THEME_KEY = 'meokji.theme'
const NOTIFY_KEY = 'meokji.notify'

const PrefsContext = createContext(null)

// 처음 켤 때 어떤 테마로 시작할지 정한다.
// 예전에 고른 값이 있으면 그걸 쓰고, 없으면 컴퓨터 설정을 따라간다.
function getInitialTheme() {
  const saved = localStorage.getItem(THEME_KEY)

  if (saved === 'light' || saved === 'dark') {
    return saved
  }

  // matchMedia 는 CSS 미디어 쿼리를 자바스크립트에서 물어보는 함수다
  if (window.matchMedia) {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    if (prefersDark) {
      return 'dark'
    }
  }

  return 'light'
}

// 알림은 기본이 켜짐. 'off' 라고 저장돼 있을 때만 꺼진 채로 시작한다.
function getInitialNotify() {
  if (localStorage.getItem(NOTIFY_KEY) === 'off') {
    return false
  }
  return true
}

export function PrefsProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme)
  const [notify, setNotify] = useState(getInitialNotify)

  // 테마가 바뀌면 <html> 에 반영하고 저장한다.
  // dataset.theme = 'dark' 를 하면 <html data-theme="dark"> 가 된다.
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  // 알림 설정이 바뀌면 저장한다
  useEffect(() => {
    if (notify) {
      localStorage.setItem(NOTIFY_KEY, 'on')
    } else {
      localStorage.setItem(NOTIFY_KEY, 'off')
    }
  }, [notify])

  const value = {
    theme: theme,
    setTheme: setTheme,
    notify: notify,
    setNotify: setNotify,
  }

  return <PrefsContext.Provider value={value}>{children}</PrefsContext.Provider>
}

export function usePrefs() {
  const context = useContext(PrefsContext)

  if (context === null) {
    throw new Error('usePrefs()는 <PrefsProvider> 안에서만 쓸 수 있습니다.')
  }

  return context
}
