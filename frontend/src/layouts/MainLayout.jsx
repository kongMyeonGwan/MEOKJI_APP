import { Outlet } from 'react-router-dom'
import Header from '../components/common/Header'

/** Header + 페이지 본문 공통 레이아웃. */
export default function MainLayout() {
  return (
    <>
      <Header />
      <main className="container" style={{ padding: '24px 20px 60px' }}>
        <Outlet />
      </main>
    </>
  )
}
