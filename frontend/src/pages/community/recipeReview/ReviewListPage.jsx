import { useState } from 'react'
import { Link } from 'react-router-dom'
import ReviewCard from './components/ReviewCard'
import CommunityTabs from '../components/CommunityTabs'
import Pagination from '../../../components/common/Pagination'
import { SAMPLE_REVIEWS } from '../../../data/sampleBoard'

// 레시피리뷰 목록 (/community).
//
// 위쪽 탭(레시피 추천 · 공지사항 · 자유게시판)은 세 화면이 같이 쓰는 조각이다
// — community/components/CommunityTabs. 사이드바에는 '커뮤니티' 하나만 있고,
// 게시판 사이는 이 탭으로 옮겨 다닌다.
//
// 퍼블리싱만 된 화면이다. 가데이터를 그리고, 페이지 번호 색만 바뀐다.
//
// TODO(게시판 담당)
//   - 목록을 getReviews(keyword, page) 로 받아온다 (reviews · totalPages 를 state 로)
//   - 검색창에 값을 묶어 keyword 로 넘긴다
//   - 글쓰기 버튼을 로그인했을 때만 보이게 한다
//     (App.jsx 의 <Route> 에서 user={user} 를 넘겨받아 판단한다)
//   - 로딩 · 오류 · 빈 목록 상태를 붙인다 (CODE_STYLE 3번)
export default function ReviewListPage() {
  const [page, setPage] = useState(1)

  const reviews = SAMPLE_REVIEWS
  const totalPages = 3 // 가데이터. 버튼 모양만 보여준다

  return (
    <div className="card">
      <div className="community__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          레시피 추천
        </h2>

        <Link to="/community/write" className="btn btn--sm">
          글쓰기
        </Link>
      </div>

      <p className="muted community__desc">요리와 레시피에 대한 정보를 나누고 소통하는 공간입니다.</p>

      <input type="search" className="community__search" placeholder="제목이나 내용으로 검색" />

      <div className="divided community__list">
        {reviews.map((review) => (
          <ReviewCard key={review.id} review={review} />
        ))}
      </div>

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
