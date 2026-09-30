// 테마(밝게/어둡게)랑 알림 켜기/끄기. 브라우저(localStorage)에 저장한다.
//
// 테마는 <html data-theme="dark"> 만 바꾸면
// index.css 의 CSS 변수가 바뀌어서 화면 전체 색이 따라 바뀐다.

const THEME_KEY = 'meokji.theme'
const NOTIFY_KEY = 'meokji.notify'

// 저장된 테마. 없으면 'light'
export const getSavedTheme = () => {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'dark') {
    return 'dark'
  }
  return 'light'
}

// 테마를 화면에 적용하고 저장한다
export const applyTheme = (theme) => {
  document.documentElement.dataset.theme = theme
  localStorage.setItem(THEME_KEY, theme)
}

// 알림은 기본이 켜짐. 'off' 로 저장돼 있을 때만 꺼짐.
export const getSavedNotify = () => {
  const saved = localStorage.getItem(NOTIFY_KEY)
  if (saved === 'off') {
    return false
  }
  return true
}

export const saveNotify = (on) => {
  localStorage.setItem(NOTIFY_KEY, on ? 'on' : 'off')
}
