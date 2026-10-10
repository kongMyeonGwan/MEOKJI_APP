// 테마(밝게/어둡게)랑 알림 켜기/끄기. 브라우저(localStorage)에 저장한다.
//
// 테마는 <html data-theme="dark"> 만 바꾸면
// index.css 의 CSS 변수가 바뀌어서 화면 전체 색이 따라 바뀐다.
//
// 그래서 색을 쓰는 컴포넌트는 테마를 몰라도 된다. 컴포넌트마다
// "지금 어두운가" 를 보고 색을 고르게 하면, 화면을 하나 더할 때마다
// 그 처리를 또 적어야 한다.
//
// 서버에 저장하지 않는 이유 — 기기마다 다를 수 있는 값이고,
// 로그인 안 한 사람도 바꿀 수 있어야 한다

const THEME_KEY = 'meokji.theme'
const NOTIFY_KEY = 'meokji.notify'

// 저장된 테마. 없으면 'light'
//
// 'dark' 인지만 보고 나머지는 전부 'light' 로 본다. 저장된 값이
// 깨져 있거나 없어도 화면이 뜨게 하려는 것이다 - 돌려주는 값이
// 둘 중 하나로 고정되므로 부르는 쪽이 확인할 것이 없다
export const getSavedTheme = () => {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'dark') {
    return 'dark'
  }
  return 'light'
}

// 테마를 화면에 적용하고 저장한다
//
// 적용과 저장을 한 함수로 묶은 이유 — 따로 두면 한쪽만 부르는 실수가
// 난다. 적용만 하면 새로고침할 때 돌아가고, 저장만 하면 지금 화면이 안 바뀐다
//
// document.documentElement 가 <html> 태그다.
// dataset.theme 에 넣으면 data-theme 속성이 된다
export const applyTheme = (theme) => {
  document.documentElement.dataset.theme = theme
  localStorage.setItem(THEME_KEY, theme)
}

// 알림은 기본이 켜짐. 'off' 로 저장돼 있을 때만 꺼짐.
//
// 켜짐을 기본으로 둔 이유 — 저장된 값이 없는 사람(처음 온 사람)에게
// 알림이 안 보이면 그런 기능이 있는 줄도 모른다.
//
// true/false 가 아니라 'on'/'off' 글자로 저장하는 이유 —
// localStorage 는 글자만 담는다. true 를 넣으면 "true" 라는 글자가
// 되어, 꺼낼 때 다시 참/거짓으로 바꿔야 한다
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
