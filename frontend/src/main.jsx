// 앱이 시작되는 곳. index.html 의 <div id="root"> 안에 App 을 그린다.
//
// 이 파일에 로직을 두지 않는다. 여기서 하는 일은 셋뿐이다 -
// 테마를 먼저 적용하고, CSS 를 불러오고, App 을 그린다.
//
// 화면을 넘기는 것(라우터)과 로그인 상태는 App.jsx 가 맡는다.

import { createRoot } from 'react-dom/client'
import App from './App'
import { applyTheme, getSavedTheme } from './utils/settings'
import './styles/index.css'

// 저장해둔 테마를 화면이 뜨기 전에 먼저 적용한다
//
// App 안의 useEffect 에서 해도 되지만, 그러면 화면이 한 번 그려진
// 뒤에 적용된다. 어두운 테마를 쓰는 사람에게 흰 화면이 번쩍하고
// 지나간다 - render 전에 <html> 을 먼저 바꿔두면 그럴 일이 없다
applyTheme(getSavedTheme())

// StrictMode 로 감싸지 않았다. 감싸면 개발 중에 useEffect 가 두 번
// 실행되는데, 서버를 부르는 useEffect 가 많아서 요청도 두 번씩 나간다.
// 조회수를 올리는 자리에서는 숫자가 2씩 오른다
createRoot(document.getElementById('root')).render(
  <App />
)
