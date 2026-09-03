import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import PostCard from '../components/community/PostCard'
import NoticeList from '../components/community/NoticeList'
import Icon from '../components/common/Icon'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import { useAuth } from '../contexts/AuthContext'
import { getPosts } from '../api/posts'
import { parseApiError } from '../utils/apiError'
import { MOCK_NOTICES } from '../mocks'
import '../styles/pages/CommunityPage.css'

/**
 * 게시판 목록 (STEP 20).
 *
 * 조회는 Guest도 가능하고, 글쓰기는 로그인한 사용자만 할 수 있다.
 * 그래서 이 라우트는 ProtectedRoute로 감싸지 않는다.
 *
 * 탭은 두 개다.
 *   recipe  레시피 추천 — DB의 posts 테이블. 검색 · 글쓰기 있음
 *   notice  공지사항     — 운영자만 쓴다. notices 테이블이 아직 없어 목업 그대로다
 */
const TABS = [
  { key: 'recipe', label: '레시피 추천', icon: 'sparkles' },
  { key: 'notice', label: '공지사항', icon: 'bell' },
]

export default function CommunityPage() {
  const { isAuthenticated } = useAuth()
  const [tab, setTab] = useState('recipe')
  const [keyword, setKeyword] = useState('')

  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const isRecipeTab = tab === 'recipe'

  // 검색은 서버가 한다. Oracle에는 ILIKE가 없어 LOWER(col) LIKE LOWER(:kw)로 처리된다.
  // keyword를 의존성에 넣어 입력이 바뀔 때마다 다시 조회한다.
  // 타이핑마다 요청이 나가지 않도록 300ms 쉰 뒤에 보낸다.
  useEffect(() => {
    let alive = true
    const timer = setTimeout(() => {
      setLoadError('')
      getPosts(keyword)
        .then((data) => {
          if (alive) setPosts(data)
        })
        .catch((err) => {
          if (alive) setLoadError(parseApiError(err).message)
        })
        .finally(() => {
          if (alive) setLoading(false)
        })
    }, 300)

    return () => {
      alive = false
      clearTimeout(timer)
    }
  }, [keyword])

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
            {t.key === 'notice' && (
              <span className="community__tab-count">{MOCK_NOTICES.length}</span>
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
              {keyword.trim() ? '검색 결과가 없습니다.' : '아직 게시글이 없습니다.'}
            </p>
          ) : (
            <div className="divided community__list">
              {posts.map((post) => (
                <PostCard key={post.id} post={post} />
              ))}
            </div>
          )}
        </>
      ) : (
        // 공지는 다섯 건뿐이라 검색을 붙이지 않았다
        <NoticeList notices={MOCK_NOTICES} />
      )}
    </div>
  )
}
