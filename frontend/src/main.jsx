// 앱이 시작되는 곳. index.html 의 <div id="root"> 안에 App 을 그린다.

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './styles/index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>
)
