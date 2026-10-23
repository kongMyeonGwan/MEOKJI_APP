
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ReviewCard from './components/ReviewCard'
import Pagination from '../../../components/common/Pagination'
import Loading from '../../../components/common/Loading'
import ErrorMessage from '../../../components/common/ErrorMessage'
import { getReviewPosts } from '../../../api/reviews'

export default function ReviewListPage() {
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [searched, setSearched] = useState('')
  const [reviews, setReviews] = useState([])
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const data = await getReviewPosts(searched, page)
        if (active) {
          setReviews(data.items || [])
          setTotalPages(data.total_pages || 1)
        }
      } catch (err) {
        if (!active) return
        if (err.response) {
          const detail = err.response.data?.detail
          setError(Array.isArray(detail) ? detail[0]?.msg : detail || '글을 불러오지 못했습니다.')
        } else if (err.request) {
          setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
        } else {
          setError(err.message)
        }
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [searched, page])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(1)
    setSearched(keyword.trim())
  }

  return (
    <div className="card">
      <div className="community__head">
        <h2 className="section-title" style={{ margin: 0 }}>레시피 추천</h2>
        <Link to="/reviews/write" className="btn btn--sm">글쓰기</Link>
      </div>
      <p className="muted community__desc">요리와 레시피에 대한 정보를 나누고 소통하는 공간입니다.</p>
<<<<<<< HEAD

  {/* <CommunityTabs /> */ }

      <input
        type="search"
        className="community__search"
        placeholder="제목이나 내용으로 검색"
        value={search}
        onChange={(e) => {
          setSearch(e.target.value)
          setPage(1)
        }}
      />

      <div className="divided community__list">
        {reviews.map((review) => (
          <ReviewCard key={review.id} review={review} />
        ))}
      </div>

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
=======
      <form onSubmit={handleSearch}>
        <input type="search" className="community__search" placeholder="제목이나 내용으로 검색 (Enter)"
          value={keyword} onChange={(e) => setKeyword(e.target.value)} maxLength={100} />
      </form>
      {loading && <Loading message="글을 불러오는 중..." />}
      <ErrorMessage message={error} />
      {!loading && !error && reviews.length === 0 && (
        <p className="empty">{searched ? '검색 결과가 없습니다.' : '아직 등록된 글이 없습니다. 첫 글을 남겨보세요.'}</p>
      )}
      {!loading && !error && reviews.length > 0 && (
        <>
          <div className="divided community__list">
            {reviews.map((review) => <ReviewCard key={review.id} review={review} />)}
          </div>
          <Pagination page={page} totalPages={totalPages} onChange={setPage} />
        </>
      )}
>>>>>>> feature/JuHwang
    </div >
  )
}
