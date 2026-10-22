import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
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
// user 는 App 이 내려주는 로그인한 사람이다. 비로그인이면 null 이고,
// 글쓰기 버튼 대신 로그인 안내를 보여주는 데에만 쓴다.
// 목록에는 수정 · 삭제가 없으므로 "내 글인지" 는 여기서 따지지 않는다
// (서버 응답에 is_mine 칸 자체가 없다 — 그건 상세에만 온다).
//
// 레시피 후기(/community)와 다른 점
//   후기는 내 식사 기록을 하나 골라야 글을 쓸 수 있지만, 여기는 그 조건이 없다.
export default function FreeListPage({ user }) {
  // 검색어와 페이지는 state 가 아니라 주소에 둔다 (/free?keyword=두부&page=3).
  // state 에만 두면 3페이지에서 글을 열었다가 뒤로 오거나 새로고침했을 때
  // 1페이지로 돌아간다. 주소에 있으면 브라우저가 기억해 준다
  const [searchParams, setSearchParams] = useSearchParams()
  const searched = searchParams.get('keyword') || '' // 실제로 서버에 보낸 검색어

  // 주소의 page 는 사람이 고쳐 넣을 수도 있다 (?page=abc, ?page=0).
  // 서버는 1 미만을 400 으로 막으므로 여기서 1 로 맞춘다
  const pageParam = Number(searchParams.get('page'))
  const page = Number.isInteger(pageParam) && pageParam >= 1 ? pageParam : 1

  // 입력칸 글자와 "실제로 검색한 말" 을 따로 둔다. 하나로 쓰면 한 글자 칠
  // 때마다 useEffect 가 돌아 서버를 계속 부른다. Enter 를 누를 때만 주소에
  // 옮겨 담아서 그때 한 번만 받아오게 한다
  const [keyword, setKeyword] = useState(searched) // 입력칸에 치고 있는 글자

  const [posts, setPosts] = useState([])
  const [totalPages, setTotalPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 주소를 바꾼다. 기본값(검색어 없음 · 1페이지)은 주소에 적지 않는다
  const moveTo = (nextKeyword, nextPage, replace = false) => {
    const params = {}
    if (nextKeyword) {
      params.keyword = nextKeyword
    }
    if (nextPage > 1) {
      params.page = String(nextPage)
    }
    setSearchParams(params, { replace: replace })
  }

  // 뒤로 가기로 주소의 검색어가 바뀌면 입력칸도 맞춘다
  useEffect(() => {
    setKeyword(searched)
  }, [searched])

  // 지금 보고 있는 목록 주소. 글을 열 때 같이 넘겨서, 상세의 "목록" 버튼이
  // 1페이지가 아니라 보던 페이지로 돌아오게 한다
  const query = searchParams.toString()
  const listUrl = query ? `/free?${query}` : '/free'

  // 검색어나 페이지가 바뀌면 다시 받아온다
  useEffect(() => {
    const load = async () => {
      setLoading(true)
      let moved = false // 마지막 페이지로 옮겼는지
      try {
        const data = await getFreePosts(searched, page)

        // 없는 페이지면 (?page=99, 또는 마지막 페이지의 글이 지워진 경우)
        // 빈 목록과 함께 페이지 버튼까지 사라져서 빠져나올 길이 없다.
        // 마지막 페이지로 옮긴다. replace 라서 뒤로 가기에 99페이지가 남지 않는다
        if (data.total_pages > 0 && page > data.total_pages) {
          moved = true
          moveTo(searched, data.total_pages, true)
          return
        }

        setPosts(data.items || [])
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
        // 옮겼으면 새 페이지를 받아올 때까지 로딩을 그대로 둔다.
        // 여기서 끄면 그 사이에 "아직 글이 없습니다" 가 잠깐 보인다
        if (!moved) {
          setLoading(false)
        }
      }
    }
    load()
  }, [searched, page])

  // 검색. 2페이지를 보다가 검색하면 1페이지부터 다시 본다
  const handleSearch = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    moveTo(keyword.trim(), 1)
  }

  const changePage = (nextPage) => {
    moveTo(searched, nextPage)
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

      {/* 셋을 따로 그린다. 불러오는 중에 "글이 없습니다" 가 잠깐 보이거나,
          오류가 났는데 빈 목록 안내까지 같이 나오는 것을 막는다 */}
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
              <FreePostCard key={post.id} post={post} listUrl={listUrl} />
            ))}
          </div>

          <Pagination page={page} totalPages={totalPages} onChange={changePage} />
        </>
      )}
    </div>
  )
}
