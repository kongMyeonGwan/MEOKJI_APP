import { useState } from 'react'
import { Link } from 'react-router-dom'
import ReviewCard from './components/ReviewCard'
import NoticeList from './components/NoticeList'
import NoticeForm from './components/NoticeForm'
import Pagination from '../../components/common/Pagination'
import Icon from '../../components/common/Icon'
import { SAMPLE_NOTICES, SAMPLE_REVIEWS } from '../../data/sampleBoard'
import './CommunityPage.css'

// 커뮤니티 목록. 탭 두 개: 레시피 추천(게시글) / 공지사항
//
// TODO(게시판 담당): 지금은 가데이터(SAMPLE_REVIEWS, SAMPLE_NOTICES)를 그린다.
//   1. reviews, notices 를 useState 로 바꾸고
//   2. useEffect 에서 getReviews(keyword, page), getNotices({ page }) 를 불러 넣는다
//   3. 공지 저장/삭제(handleNoticeSaved, handleNoticeDelete) 에서 API 를 부른다
export default function CommunityPage({ user }) {
  const [tab, setTab] = useState('recipe') // 'recipe' 또는 'notice'
  const [keyword, setKeyword] = useState('') // 게시글 검색어
  const [page, setPage] = useState(1)

  // 공지 폼 상태: null(닫힘) / 'new'(새 공지) / 공지 객체(그 공지 수정 중)
  const [editingNotice, setEditingNotice] = useState(null)

  // 관리자인지 (관리자만 공지를 쓸 수 있다)
  const isAdmin = user && user.is_admin

  const reviews = SAMPLE_REVIEWS
  const notices = SAMPLE_NOTICES
  const totalPages = 1

  const handleNoticeSaved = (form, editing) => {
    setEditingNotice(null)
    alert('공지사항 기능은 아직 준비 중입니다.')
  }

  const handleNoticeDelete = (notice) => {
    alert('공지사항 기능은 아직 준비 중입니다.')
  }

  const changePage = (nextPage) => {
    setPage(nextPage)
    window.scrollTo(0, 0)
  }

  return (
    <div className="card">
      <div className="community__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          커뮤니티
        </h2>

        {/* 레시피 탭: 로그인했으면 글쓰기, 아니면 로그인 안내 */}
        {tab === 'recipe' && user && (
          <Link to="/community/write" className="btn btn--sm">
            글쓰기
          </Link>
        )}
        {tab === 'recipe' && !user && (
          <Link to="/login" className="muted community__hint">
            로그인하고 글쓰기 →
          </Link>
        )}

        {/* 공지 탭: 관리자만 공지 작성 */}
        {tab === 'notice' && isAdmin && editingNotice === null && (
          <button type="button" className="btn btn--sm" onClick={() => setEditingNotice('new')}>
            공지 작성
          </button>
        )}
      </div>

      <p className="muted community__desc">
        {tab === 'recipe'
          ? '요리와 레시피에 대한 정보를 나누고 소통하는 공간입니다.'
          : '서비스 점검 · 업데이트 소식을 안내해드립니다.'}
      </p>

      <div className="community__tabs">
        <button
          type="button"
          className={tab === 'recipe' ? 'community__tab is-active' : 'community__tab'}
          onClick={() => setTab('recipe')}
        >
          <Icon name="sparkles" size={16} />
          레시피 추천
        </button>
        <button
          type="button"
          className={tab === 'notice' ? 'community__tab is-active' : 'community__tab'}
          onClick={() => setTab('notice')}
        >
          <Icon name="bell" size={16} />
          공지사항
          {notices.length > 0 && <span className="community__tab-count">{notices.length}</span>}
        </button>
      </div>

      {/* 레시피 추천 탭 */}
      {tab === 'recipe' && (
        <>
          <input
            type="search"
            className="community__search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="제목이나 내용으로 검색"
          />

          {reviews.length === 0 ? (
            <p className="empty">아직 게시글이 없습니다.</p>
          ) : (
            <>
              <div className="divided community__list">
                {reviews.map((review) => (
                  <ReviewCard key={review.id} review={review} />
                ))}
              </div>
              <Pagination page={page} totalPages={totalPages} onChange={changePage} />
            </>
          )}
        </>
      )}

      {/* 공지사항 탭 */}
      {tab === 'notice' && (
        <>
          {editingNotice !== null && (
            <NoticeForm
              // key 가 바뀌면 폼을 새로 만든다 (다른 공지를 수정할 때 입력칸을 새로 채우려고)
              key={editingNotice === 'new' ? 'new' : editingNotice.id}
              notice={editingNotice === 'new' ? null : editingNotice}
              onDone={handleNoticeSaved}
              onCancel={() => setEditingNotice(null)}
            />
          )}

          <NoticeList notices={notices} canManage={isAdmin} onEdit={setEditingNotice} onDelete={handleNoticeDelete} />
        </>
      )}
    </div>
  )
}
