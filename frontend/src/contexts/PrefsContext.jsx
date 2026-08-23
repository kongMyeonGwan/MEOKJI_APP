import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

/**
 * 환경 설정 — 화면 테마와 알림 수신 여부.
 *
 * 로그인과 무관하게 이 브라우저에만 적용되는 값이라 서버에 보내지 않고
 * localStorage에만 둔다. 비로그인 상태에서도 다크 모드를 쓸 수 있어야 한다.
 *
 * 테마는 CSS 변수로만 갈라진다(styles/index.css). 색을 컴포넌트마다 분기하지 않고
 * <html data-theme="dark"> 한 곳만 바꾸면 전체가 따라오게 해두면,
 * 새 화면을 만들 때 다크 모드를 따로 신경 쓸 일이 없다.
 */

const THEME_KEY = 'meokji.theme'
const NOTIFY_KEY = 'meokji.notify'

const PrefsContext = createContext(null)

/**
 * 저장된 테마가 없으면 OS 설정을 따른다.
 * 처음 들어온 사람에게 "다크 모드 쓰는 사람이구나"를 한 번은 맞춰주고,
 * 그다음부터는 직접 고른 값이 우선이다.
 */
function initialTheme() {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function PrefsProvider({ children }) {
  const [theme, setTheme] = useState(initialTheme)
  // 기본은 켜짐. 꺼두면 상단바 종의 숫자 배지가 사라진다
  const [notify, setNotify] = useState(() => localStorage.getItem(NOTIFY_KEY) !== 'off')

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  useEffect(() => {
    localStorage.setItem(NOTIFY_KEY, notify ? 'on' : 'off')
  }, [notify])

  const toggleTheme = useCallback(() => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'))
  }, [])

  const value = useMemo(
    () => ({ theme, setTheme, toggleTheme, notify, setNotify }),
    [theme, toggleTheme, notify]
  )

  return <PrefsContext.Provider value={value}>{children}</PrefsContext.Provider>
}

export function usePrefs() {
  const ctx = useContext(PrefsContext)
  if (ctx === null) {
    throw new Error('usePrefs()는 <PrefsProvider> 안에서만 쓸 수 있습니다.')
  }
  return ctx
}
