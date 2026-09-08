import { createContext, useContext, useEffect, useState } from 'react'

/**
 * 환경 설정 — 화면 테마(밝게/어둡게)와 알림 수신 여부.
 *
 * 로그인과 상관없이 이 브라우저에만 적용되는 값이라 서버에 보내지 않고
 * localStorage 에만 저장한다. 로그인 안 한 사람도 다크 모드를 쓸 수 있어야 한다.
 *
 * 테마를 바꾸는 방법
 * ------------------
 * 색을 컴포넌트마다 if 로 나누지 않는다.
 * <html data-theme="dark"> 한 곳만 바꾸면, styles/index.css 에 정의해둔
 * CSS 변수가 통째로 바뀌면서 화면 전체가 따라온다.
 * 그래서 새 화면을 만들 때 다크 모드를 따로 신경 쓸 일이 없다.
 *
 * 쓰는 법
 * -------
 *   const { theme, toggleTheme, notify, setNotify } = usePrefs()
 */

const THEME_KEY = 'meokji.theme'
const NOTIFY_KEY = 'meokji.notify'

const PrefsContext = createContext(null)

/**
 * 처음 켤 때 어떤 테마로 시작할지 정한다.
 *
 * 1) 예전에 직접 고른 값이 있으면 그걸 쓴다
 * 2) 없으면 OS 설정을 따라간다 (윈도우/맥의 다크 모드 설정)
 *
 * 다크 모드를 쓰는 사람에게 한 번은 맞춰주고,
 * 그다음부터는 이 앱에서 직접 고른 값이 우선이다.
 */
function getInitialTheme() {
  const saved = localStorage.getItem(THEME_KEY)

  if (saved === 'light' || saved === 'dark') {
    return saved
  }

  // matchMedia 는 CSS 의 미디어 쿼리를 자바스크립트에서 물어보는 함수다.
  // ?. 는 "matchMedia 가 없는 옛날 브라우저면 그냥 넘어가라" 는 뜻이다.
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches

  if (prefersDark) {
    return 'dark'
  }
  return 'light'
}

/** 알림은 기본이 켜짐. 'off' 라고 저장해둔 경우에만 꺼진 상태로 시작한다. */
function getInitialNotify() {
  return localStorage.getItem(NOTIFY_KEY) !== 'off'
}

export function PrefsProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme)
  const [notify, setNotify] = useState(getInitialNotify)

  // 테마가 바뀌면 <html> 태그에 반영하고 저장한다.
  // dataset.theme = 'dark'  ->  <html data-theme="dark">
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  // 알림 설정이 바뀌면 저장한다
  useEffect(() => {
    localStorage.setItem(NOTIFY_KEY, notify ? 'on' : 'off')
  }, [notify])

  /** 밝게 <-> 어둡게 전환. */
  function toggleTheme() {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'))
  }

  const value = {
    theme,
    setTheme,
    toggleTheme,
    notify,
    setNotify,
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
