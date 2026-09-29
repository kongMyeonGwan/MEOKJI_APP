import { useState } from 'react'
import { Link } from 'react-router-dom'
import ReviewCard from '../../components/community/ReviewCard'
import NoticeList from '../../components/community/NoticeList'
import NoticeForm from '../../components/community/NoticeForm'
import Pagination from '../../components/common/Pagination'
import Icon from '../../components/common/Icon'
import { useAuth } from '../../contexts/AuthContext'
import { SAMPLE_NOTICES, SAMPLE_REVIEWS } from '../../data/sampleBoard'
import '../../styles/pages/CommunityPage.css'

// 커뮤니티 목록 화면.
//
// 탭이 두 개다.
//   recipe - 레시피 추천. 검색이랑 글쓰기가 있다.    111
//   notice - 공지사항. 관리자만 쓸 수 있다.
//
// ! 아직 서버에 붙어 있지 않다. 담당자가 채운다.
//
// 지금은 src/data/sampleBoard.js 의 하드코딩 데이터를 그린다.
// 화면 모양(마크업 · CSS)은 완성돼 있으니 아래 TODO 자리만 채우면 된다.
//
// 붙일 때 할 일
// -------------
//   1. useEffect 로 getReviews(query, page) · getNotices({ page }) 를 부른다
//   2. 응답의 items 를 reviews / notices 에 넣고 total_pages 도 같이 받는다
//   3. 검색은 이미 아래 keyword -> query 로 0.3초 늦춰 놨으니 그대로 쓰면 된다
//   4. 공지 쓰기 · 고치기 · 지우기는 NoticeForm / NoticeList 가
//      onDone · onEdit · onDelete 로 알려준다

const TABS = [
  { key: 'recipe', label: '레시피 추천', icon: 'sparkles' },
  { key: 'notice', label: '공지사항', icon: 'bell' },
]

export default function CommunityPage() {
  const { isAuthenticated, user } = useAuth()
  const [tab, setTab] = useState('recipe')

  // 검색어를 두 개 들고 있는 이유
  //   keyword - 입력칸에 지금 찍혀 있는 글자. 칠 때마다 바뀐다.
  //   query   - 실제로 서버에 보낼 검색어. 타이핑이 멎은 뒤에 따라온다.
  //
  // 하나로 두면 "김치찌개" 를 칠 때 글자마다 요청이 나가서 5번 나간다.
  const [keyword, setKeyword] = useState('')
  const [query, setQuery] = useState('')

  const [page, setPage] = useState(1)
  const [noticePage, setNoticePage] = useState(1)

  // 열려 있는 공지 폼.
  //   null   - 닫혀 있다
  //   'new'  - 새 공지 작성
  //   객체   - 그 공지를 수정하는 중
  const [editing, setEditing] = useState(null)

  const isRecipeTab = tab === 'recipe'

  // 관리자한테만 쓰기 버튼이 보인다.
  // 숨기는 건 잠그는 게 아니고 진짜 차단은 서버가 한다.
  let canManage = false
  if (user && user.is_admin) {
    canManage = true
  }

  // TODO: 서버에서 받아오게 바꾼다. 지금은 하드코딩 데이터다.
  //   const [reviews, setPosts] = useState([])
  //   const [totalPages, setTotalPages] = useState(1)
  //   useEffect(() => { getReviews(query, page).then(...) }, [query, page])
  const reviews = SAMPLE_REVIEWS
  const totalPages = 1

  // TODO: 공지도 마찬가지다. getNotices({ page: noticePage })
  const notices = SAMPLE_NOTICES
  const noticeTotalPages = 1

  // 타이핑이 멎으면 0.3초 뒤에 검색어를 query 로 옮긴다.
  //
  // TODO: 서버 검색이 붙으면 이 useEffect 를 살린다.
  //   useEffect(() => {
  //     const timer = setTimeout(function () {
  //       setQuery(keyword)
  //       setPage(1)
  //     }, 300)
  //     return function () { clearTimeout(timer) }
  //   }, [keyword])

  // 공지를 쓰거나 고친 뒤에 목록을 서버에서 다시 받아온다.
  //
  // 응답으로 온 공지를 배열에 직접 끼워 넣지 않는다.
  // 고정을 켜면 맨 위로 가야 하는데 그 정렬은 서버가 정하는 거라
  // 화면이 흉내 내면 새로고침하는 순간 순서가 달라진다.
  function onNoticeSaved(saved, wasEditing) {
    setEditing(null)

    // TODO: reloadNotices() 를 만들어 부른다.
    //   새로 쓴 공지는 최신이라 1페이지에 있으므로 setNoticePage(1) 도 같이 한다.
    window.alert('공지사항 기능은 아직 준비 중입니다.')
  }

  // 공지 삭제. 확인창은 NoticeList 가 이미 띄웠다.
  function onNoticeDelete(notice) {
    // TODO: deleteNotice(notice.id) 를 부르고 목록을 다시 받아온다.
    window.alert('공지사항 기능은 아직 준비 중입니다.')
  }

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
            레시피 탭은 회원이면 누구나 쓰고 공지 탭은 관리자만 쓴다. */}
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

          {reviews.length === 0 && <p className="empty">{emptyText}</p>}

          {reviews.length > 0 && (
            <>
              <div className="divided community__list">
                {reviews.map(function (review) {
                  return <ReviewCard key={review.id} review={review} />
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
