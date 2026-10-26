import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import NoticeCard from './components/NoticeCard'
import CommunityTabs from '../components/CommunityTabs'
import Pagination from '../../../components/common/Pagination'
import { getNotices } from '../../../api/notices' // ★ noticeApi → notices

// 공지사항 목록 (/notice).
// 보기는 누구나 되고, 작성 · 수정 · 삭제는 관리자만 된다.
// 고정 공지(pinned)가 위, 그 다음 최신순으로 서버가 정렬해서 준다.
export default function NoticeListPage({ user }) {
  const [page, setPage] = useState(1)
  const [notices, setNotices] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // page 가 바뀔 때마다 서버에서 다시 받아온다
  useEffect(() => {
    // 페이지를 빠르게 넘겼을 때 늦게 도착한 옛 응답이 화면을 덮어쓰지 않게 한다
    let cancelled = false

    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const data = await getNotices({ page })
        if (cancelled) {
          return
        }
        setNotices(data.items)
        setTotalPages(data.totalPages)
      } catch (e) {
        if (cancelled) {
          return
        }
        setError('공지사항을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.')
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    load()

    return () => {
      cancelled = true
    }
  }, [page])

  // 로그인하지 않았으면 user 가 null 이다
  const isAdmin = Boolean(user?.isAdmin)

  return (
    <div className="card">
      <div className="notice-page__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          공지사항
        </h2>

        {isAdmin && (
          <Link to="/notice/write" className="btn btn--sm">
            공지 작성
          </Link>
        )}
      </div>

      <p className="muted notice-page__desc">서비스 점검 · 업데이트 소식을 안내해드립니다.</p>

      <CommunityTabs />

      {loading && <p className="muted">불러오는 중입니다...</p>}
      {error && <p className="muted">{error}</p>}
      {!loading && !error && notices.length === 0 && <p className="muted">등록된 공지가 없습니다.</p>}

      <ul className="notice-list">
        {notices.map((notice) => (
          <NoticeCard key={notice.id} notice={notice} />
        ))}
      </ul>

      {totalPages > 0 && <Pagination page={page} totalPages={totalPages} onChange={setPage} showSingle />}
    </div>
  )
}
