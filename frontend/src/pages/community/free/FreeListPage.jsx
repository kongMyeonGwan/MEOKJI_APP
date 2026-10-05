import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FreePostCard from './components/FreePostCard'
import CommunityTabs from '../components/CommunityTabs'
import Pagination from '../../../components/common/Pagination'
import Loading from '../../../components/common/Loading'
import ErrorMessage from '../../../components/common/ErrorMessage'
import Icon from '../../../components/common/Icon'
import { getFreePosts } from '../../../api/freePosts'

// 자유게시판 목록 (/free).
// 보는 것은 로그인 없이 되고, 글쓰기는 로그인해야 된다.
//
// 레시피 후기(/community)와 다른 점
//   후기는 내 식사 기록을 하나 골라야 글을 쓸 수 있지만, 여기는 그 조건이 없다.
export default function FreeListPage({ user }) {
  const [keyword, setKeyword] = useState('') // 입력칸에 치고 있는 글자
  const [searched, setSearched] = useState('') // 실제로 서버에 보낸 검색어
  const [page, setPage] = useState(1)

  const [posts, setPosts] = useState([])
  const [totalPages, setTotalPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 검색어나 페이지가 바뀌면 다시 받아온다
  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const data = await getFreePosts(searched, page)
        setPosts(data.items)
        setTotalPages(data.total_pages)
        setTotal(data.total)
        setError('')
      } catch (err) {
        // 서버가 보낸 오류 문장을 그대로 보여준다.
        if (err.response) {
          // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
          const detail = err.response.data.detail
          if (Array.isArray(detail)) {
            setError(detail[0].msg)
          } else {
            setError(detail || '요청을 처리하지 못했습니다.')
          }
        } else if (err.request) {
          // 백엔드를 안 켰거나 30초가 지난 경우
          setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
        } else {
          setError(err.message)
        }
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [searched, page])

  // 검색. 2페이지를 보다가 검색하면 1페이지부터 다시 본다
  const handleSearch = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    setPage(1)
    setSearched(keyword.trim())
  }

  const changePage = (nextPage) => {
    setPage(nextPage)
    window.scrollTo(0, 0)
  }

  return (
    <div className="card">
      <div className="free__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          자유게시판
        </h2>

        {/* 로그인했으면 글쓰기, 아니면 로그인 안내 */}
        {user ? (
          <Link to="/free/write" className="btn btn--sm">
            글쓰기
          </Link>
        ) : (
          <Link to="/login" className="muted free__hint">
            로그인하고 글쓰기 →
          </Link>
        )}
      </div>

      <p className="muted free__desc">
        <Icon name="chat" size={15} />
        주제 제한 없이 이야기를 나누는 공간입니다. 요리 이야기가 아니어도 괜찮아요.
      </p>

      <CommunityTabs />

      <form onSubmit={handleSearch}>
        <input
          type="search"
          className="free__search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="제목이나 내용으로 검색 (Enter)"
          maxLength={100}
        />
      </form>

      {loading && <Loading message="글을 불러오는 중..." />}
      <ErrorMessage message={error} />

      {!loading && !error && posts.length === 0 && (
        <p className="empty">
          {searched ? `'${searched}' 검색 결과가 없습니다.` : '아직 글이 없습니다. 첫 글을 남겨보세요.'}
        </p>
      )}

      {!loading && !error && posts.length > 0 && (
        <>
          {searched && <p className="muted free__count">검색 결과 {total}건</p>}

          <div className="divided free__list">
            {posts.map((post) => (
              <FreePostCard key={post.id} post={post} />
            ))}
          </div>

          <Pagination page={page} totalPages={totalPages} onChange={changePage} />
        </>
      )}
    </div>
  )
}
