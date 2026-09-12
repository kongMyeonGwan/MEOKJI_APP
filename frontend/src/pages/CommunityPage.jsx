import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import PostCard from '../components/community/PostCard'
import NoticeList from '../components/community/NoticeList'
import Pagination from '../components/common/Pagination'
import Icon from '../components/common/Icon'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import { useAuth } from '../contexts/AuthContext'
import { getNotices } from '../api/notices'
import { getPosts } from '../api/posts'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/CommunityPage.css'

/**
 * 게시판 목록 (STEP 20).
 *
 * 조회는 Guest도 가능하고, 글쓰기는 로그인한 사용자만 할 수 있다.
 * 그래서 이 라우트는 ProtectedRoute로 감싸지 않는다.
 *
 * 탭은 두 개다.
 *   recipe  레시피 추천 — DB의 recipe_reviews 테이블. 검색 · 글쓰기 있음
 *   notice  공지사항     — 운영자만 쓴다. 사용자가 만드는 값이 아니라
 *                          테이블 없이 서버 파일(catalog.json)에서 내려준다
 */
const TABS = [
  { key: 'recipe', label: '레시피 추천', icon: 'sparkles' },
  { key: 'notice', label: '공지사항', icon: 'bell' },
]

export default function CommunityPage() {
  const { isAuthenticated } = useAuth()
  const [tab, setTab] = useState('recipe')

  // 검색어가 둘인 이유
  // ------------------
  //   keyword  입력칸에 지금 찍혀 있는 글자. 타이핑할 때마다 바뀐다
  //   query    실제로 서버에 보낸 검색어. 타이핑이 멎은 뒤에 따라온다
  //
  // 하나로 두면 "김치찌개" 를 칠 때 글자마다 요청이 나가서 5번 나간다.
  const [keyword, setKeyword] = useState('')
  const [query, setQuery] = useState('')

  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [notices, setNotices] = useState([])

  const isRecipeTab = tab === 'recipe'

  // 공지는 검색도 페이징도 없어서 화면이 열릴 때 한 번만 받아온다.
  // 실패하면 빈 목록으로 둔다. 공지를 못 받았다고 게시판까지 막을 이유는 없다.
  useEffect(() => {
    let alive = true

    getNotices()
      .then((data) => {
        if (alive) setNotices(data)
      })
      .catch(() => {})

    return () => {
      alive = false
    }
  }, [])

  // 타이핑이 멎으면 0.3초 뒤에 검색어를 query 로 옮긴다.
  //
  // 0.3초 안에 또 치면 아래 정리 함수가 앞의 타이머를 취소하고 새로 잡는다.
  // 그래서 "김치찌개" 를 쳐도 요청은 마지막에 한 번만 나간다.
  //
  // 검색어가 바뀌면 1페이지로 되돌린다. 3페이지를 보다가 검색하면
  // "검색 결과 2건" 인데 3페이지를 달라고 해서 빈 화면이 뜬다.
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(keyword)
      setPage(1)
    }, 300)

    return () => clearTimeout(timer)
  }, [keyword])

  // 검색은 서버가 하고(post_service.add_keyword_filter), 자르는 것도 서버가 한다.
  //
  // alive 플래그가 필요한 이유
  // --------------------------
  // 요청은 보낸 순서대로 도착하지 않는다. 2페이지를 눌렀다가 3페이지를 눌렀는데
  // 2페이지 응답이 더 늦게 올 수 있고, 그러면 3페이지를 보고 있는데 목록만
  // 2페이지 것으로 덮인다. effect 가 다시 실행되기 직전에 아래 정리 함수가
  // alive 를 false 로 바꾸므로, 낡은 응답이 도착해도 setPosts 를 부르지 않는다.
  useEffect(() => {
    let alive = true

    setLoadError('')

    getPosts(query, page)
      .then((data) => {
        if (!alive) return

        setPosts(data.items)
        setTotalPages(data.total_pages)
      })
      .catch((err) => {
        if (alive) setLoadError(parseApiError(err).message)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [query, page])

  /** 페이지 버튼을 눌렀을 때. 목록이 바뀌므로 화면 맨 위로 올려준다. */
  function onPageChange(next) {
    setPage(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="card">
      <div className="community__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          커뮤니티
        </h2>

        {/* 공지 탭에서는 글쓰기를 숨긴다. 공지는 운영자만 쓸 수 있다 */}
        {isRecipeTab &&
          (isAuthenticated ? (
            <Link to="/community/write" className="btn btn--sm">
              글쓰기
            </Link>
          ) : (
            <Link to="/login" className="muted community__hint">
              로그인하고 글쓰기 →
            </Link>
          ))}
      </div>

      <p className="muted community__desc">
        {isRecipeTab
          ? '요리와 레시피에 대한 정보를 나누고 소통하는 공간입니다.'
          : '서비스 점검 · 업데이트 소식을 안내해드립니다.'}
      </p>

      {/* role="tablist"를 주면 화면낭독기가 "탭 1/2" 식으로 읽어준다 */}
      <div className="community__tabs" role="tablist" aria-label="커뮤니티 분류">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={tab === t.key}
            className={`community__tab${tab === t.key ? ' is-active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            <Icon name={t.icon} size={16} />
            {t.label}
            {t.key === 'notice' && notices.length > 0 && (
              <span className="community__tab-count">{notices.length}</span>
            )}
          </button>
        ))}
      </div>

      {isRecipeTab ? (
        <>
          <input
            type="search"
            className="community__search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="제목이나 내용으로 검색"
            aria-label="게시글 검색"
          />

          {loadError ? (
            <ErrorMessage message={loadError} />
          ) : loading ? (
            <Loading message="게시글을 불러오는 중..." />
          ) : posts.length === 0 ? (
            <p className="empty">
              {query.trim() ? '검색 결과가 없습니다.' : '아직 게시글이 없습니다.'}
            </p>
          ) : (
            <>
              <div className="divided community__list">
                {posts.map((post) => (
                  <PostCard key={post.id} post={post} />
                ))}
              </div>

              {/* 한 페이지뿐이면 Pagination 이 스스로 아무것도 안 그린다 */}
              <Pagination
                page={page}
                totalPages={totalPages}
                onChange={onPageChange}
              />
            </>
          )}
        </>
      ) : (
        // 공지는 몇 건뿐이라 검색을 붙이지 않았다
        <NoticeList notices={notices} />
      )}
    </div>
  )
}
