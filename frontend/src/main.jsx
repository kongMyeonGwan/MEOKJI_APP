// 앱이 시작되는 곳. index.html 의 <div id="root"> 안에 App 을 그린다.

import { createRoot } from 'react-dom/client'
import App from './App'
import { applyTheme, getSavedTheme } from './utils/settings'
import './styles/index.css'

// 저장해둔 테마를 화면이 뜨기 전에 먼저 적용한다
applyTheme(getSavedTheme())

createRoot(document.getElementById('root')).render(
  <App />
)
