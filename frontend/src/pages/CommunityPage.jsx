import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import PostCard from '../components/community/PostCard'
import NoticeList from '../components/community/NoticeList'
import NoticeForm from '../components/community/NoticeForm'
import Pagination from '../components/common/Pagination'
import Icon from '../components/common/Icon'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import { useAuth } from '../contexts/AuthContext'
import { deleteNotice, getNotices } from '../api/notices'
import { getPosts } from '../api/posts'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/CommunityPage.css'

// 커뮤니티 목록 화면.
//
// 보는 건 로그인 없이도 되고 글쓰기는 로그인해야 한다.
//
// 탭이 두 개다.
//   recipe - 레시피 추천. 검색이랑 글쓰기가 있다.
//   notice - 공지사항. 관리자만 쓸 수 있다.

const TABS = [
  { key: 'recipe', label: '레시피 추천', icon: 'sparkles' },
  { key: 'notice', label: '공지사항', icon: 'bell' },
]

export default function CommunityPage() {
  const { isAuthenticated, user } = useAuth()
  const [tab, setTab] = useState('recipe')

  // 검색어를 두 개 들고 있는 이유
  //   keyword - 입력칸에 지금 찍혀 있는 글자. 칠 때마다 바뀐다.
  //   query   - 실제로 서버에 보낸 검색어. 타이핑이 멎은 뒤에 따라온다.
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
  const [noticePage, setNoticePage] = useState(1)
  const [noticeTotalPages, setNoticeTotalPages] = useState(1)
  const [noticeError, setNoticeError] = useState('')

  // 열려 있는 공지 폼.
  //   null   - 닫혀 있다
  //   'new'  - 새 공지 작성
  //   객체   - 그 공지를 수정하는 중
  //
  // "열렸나" 랑 "무엇을 고치나" 를 state 두 개로 두면
  // 열렸는데 대상이 없는 이상한 조합이 생긴다.
  const [editing, setEditing] = useState(null)

  const isRecipeTab = tab === 'recipe'

  // 관리자한테만 쓰기 버튼이 보인다.
  // 숨기는 건 잠그는 게 아니고 진짜 차단은 서버가 한다.
  let canManage = false
  if (user && user.is_admin) {
    canManage = true
  }

  // 공지는 검색이 없어서 페이지만 보면 된다.
  // 실패하면 빈 목록으로 둔다. 공지를 못 받았다고 게시판까지 막을 이유는 없다.
  useEffect(() => {
    let alive = true

    getNotices({ page: noticePage })
      .then(function (data) {
        if (!alive) {
          return
        }
        setNotices(data.items)
        setNoticeTotalPages(data.total_pages)
      })
      .catch(function () {})

    return function () {
      alive = false
    }
  }, [noticePage])

  // 공지를 쓰거나 고친 뒤에 목록을 서버에서 다시 받아온다.
  //
  // 응답으로 온 공지를 배열에 직접 끼워 넣지 않는다.
  // 고정을 켜면 맨 위로 가야 하는데 그 정렬은 서버가 정하는 거라
  // 화면이 흉내 내면 새로고침하는 순간 순서가 달라진다.
  async function reloadNotices(targetPage) {
    let pageToLoad = targetPage
    if (!pageToLoad) {
      pageToLoad = noticePage
    }

    try {
      const data = await getNotices({ page: pageToLoad })
      setNotices(data.items)
      setNoticeTotalPages(data.total_pages)

      // 마지막 공지를 지워서 페이지가 통째로 사라졌으면 한 장 앞으로
      if (data.items.length === 0 && pageToLoad > 1) {
        setNoticePage(pageToLoad - 1)
      }
    } catch (err) {
      setNoticeError(parseApiError(err).message)
    }
  }

  // 폼이 저장을 마쳤을 때.
  //
  // 새로 쓴 공지는 최신이라 1페이지에 있다.
  // 3페이지를 보다가 공지를 쓰면 저장은 됐는데 화면에는 안 보여서
  // 1페이지로 돌려준다. 수정은 원래 있던 페이지에 그대로 남는다.
  function onNoticeSaved(saved, wasEditing) {
    setEditing(null)
    setNoticeError('')

    if (wasEditing) {
      reloadNotices()
    } else {
      setNoticePage(1)
      reloadNotices(1)
    }
  }

  // 공지 삭제. 확인창은 NoticeList 가 이미 띄웠다.
  async function onNoticeDelete(notice) {
    setNoticeError('')

    try {
      await deleteNotice(notice.id)

      // 지운 공지를 고치던 중이었으면 폼도 같이 닫는다.
      // 안 닫으면 없는 공지를 수정하는 폼이 남아서 저장할 때 404 가 난다.
      if (editing && editing.id === notice.id) {
        setEditing(null)
      }

      reloadNotices()
    } catch (err) {
      setNoticeError(parseApiError(err).message)
    }
  }

  // 타이핑이 멎으면 0.3초 뒤에 검색어를 query 로 옮긴다.
  //
  // 0.3초 안에 또 치면 아래 정리 함수가 앞의 타이머를 취소하고 새로 잡는다.
  // 그래서 "김치찌개" 를 쳐도 요청은 마지막에 한 번만 나간다.
  //
  // 검색어가 바뀌면 1페이지로 되돌린다.
  // 3페이지를 보다가 검색하면 결과가 2건인데 3페이지를 달라고 해서 빈 화면이 뜬다.
  useEffect(() => {
    const timer = setTimeout(function () {
      setQuery(keyword)
      setPage(1)
    }, 300)

    return function () {
      clearTimeout(timer)
    }
  }, [keyword])

  // 검색도 자르는 것도 서버가 한다.
  //
  // alive 가 필요한 이유는 요청이 보낸 순서대로 도착하지 않기 때문이다.
  // 2페이지를 눌렀다 3페이지를 눌렀는데 2페이지 응답이 더 늦게 오면
  // 3페이지를 보고 있는데 목록만 2페이지 것이 된다.
  useEffect(() => {
    let alive = true

    setLoadError('')

    getPosts(query, page)
      .then(function (data) {
        if (!alive) {
          return
        }

        setPosts(data.items)
        setTotalPages(data.total_pages)
      })
      .catch(function (err) {
        if (alive) {
          setLoadError(parseApiError(err).message)
        }
      })
      .finally(function () {
        if (alive) {
          setLoading(false)
        }
      })

    return function () {
      alive = false
    }
  }, [query, page])

  // 페이지 버튼을 눌렀을 때. 목록이 바뀌니까 화면 맨 위로 올려준다.
  function onPageChange(next) {
    setPage(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // 공지 탭의 페이지 버튼. 탭마다 페이지를 따로 센다.
  // 게시판 3페이지를 보다 공지 탭으로 옮겼는데 공지도 3페이지면 이상하다.
  function onNoticePageChange(next) {
    setNoticePage(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  let descText = '서비스 점검 · 업데이트 소식을 안내해드립니다.'
  if (isRecipeTab) {
    descText = '요리와 레시피에 대한 정보를 나누고 소통하는 공간입니다.'
  }

  let emptyText = '아직 게시글이 없습니다.'
  if (query.trim()) {
    emptyText = '검색 결과가 없습니다.'
  }

  // 공지 폼의 key 와 대상
  let formKey = 'new'
  let formNotice = null
  if (editing !== null && editing !== 'new') {
    formKey = editing.id
    formNotice = editing
  }

  return (
    <div className="card">
      <div className="community__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          커뮤니티
        </h2>

        {/* 버튼 자리는 하나인데 탭마다 주인이 다르다.
            레시피 탭은 회원이면 누구나 쓰고 공지 탭은 관리자만 쓴다.
            공지 탭에서 '글쓰기' 를 그대로 두면 눌렀을 때 게시글 폼이 열려서
            공지를 쓰려던 사람이 엉뚱한 곳에 글을 쓰게 된다. */}
        {isRecipeTab && isAuthenticated && (
          <Link to="/community/write" className="btn btn--sm">
            글쓰기
          </Link>
        )}

        {isRecipeTab && !isAuthenticated && (
          <Link to="/login" className="muted community__hint">
            로그인하고 글쓰기 →
          </Link>
        )}

        {!isRecipeTab && canManage && editing === null && (
          <button
            type="button"
            className="btn btn--sm"
            onClick={() => setEditing('new')}
          >
            공지 작성
          </button>
        )}
      </div>

      <p className="muted community__desc">{descText}</p>

      <div className="community__tabs">
        {TABS.map(function (t) {
          let tabClass = 'community__tab'
          if (tab === t.key) {
            tabClass = 'community__tab is-active'
          }

          return (
            <button
              key={t.key}
              type="button"
              className={tabClass}
              onClick={() => setTab(t.key)}
            >
              <Icon name={t.icon} size={16} />
              {t.label}
              {t.key === 'notice' && notices.length > 0 && (
                <span className="community__tab-count">{notices.length}</span>
              )}
            </button>
          )
        })}
      </div>

      {/* 레시피 추천 탭 */}
      {isRecipeTab && (
        <>
          <input
            type="search"
            className="community__search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="제목이나 내용으로 검색"
            aria-label="게시글 검색"
          />

          {loadError && <ErrorMessage message={loadError} />}

          {!loadError && loading && <Loading message="게시글을 불러오는 중..." />}

          {!loadError && !loading && posts.length === 0 && (
            <p className="empty">{emptyText}</p>
          )}

          {!loadError && !loading && posts.length > 0 && (
            <>
              <div className="divided community__list">
                {posts.map(function (post) {
                  return <PostCard key={post.id} post={post} />
                })}
              </div>

              {/* 한 페이지뿐이면 Pagination 이 알아서 아무것도 안 그린다 */}
              <Pagination
                page={page}
                totalPages={totalPages}
                onChange={onPageChange}
              />
            </>
          )}
        </>
      )}

      {/* 공지사항 탭. 몇 건뿐이라 검색을 안 붙였다. */}
      {!isRecipeTab && (
        <>
          {/* key 를 대상마다 다르게 준다.
              안 주면 '수정' 으로 다른 공지에 갈아탈 때 리액트가 같은 폼으로 보고
              입력값을 그대로 둔다. useState 초기값은 첫 렌더에서만 쓰이기 때문이다. */}
          {editing !== null && (
            <NoticeForm
              key={formKey}
              notice={formNotice}
              onDone={onNoticeSaved}
              onCancel={() => setEditing(null)}
            />
          )}

          <ErrorMessage message={noticeError} />

          <NoticeList
            notices={notices}
            canManage={canManage}
            onEdit={setEditing}
            onDelete={onNoticeDelete}
          />

          <Pagination
            page={noticePage}
            totalPages={noticeTotalPages}
            onChange={onNoticePageChange}
          />
        </>
      )}
    </div>
  )
}
