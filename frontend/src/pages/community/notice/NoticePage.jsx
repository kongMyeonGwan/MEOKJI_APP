import { useState } from 'react'
import { Link } from 'react-router-dom'
import NoticeCard from './components/NoticeCard'
import CommunityTabs from '../components/CommunityTabs'
import Pagination from '../../../components/common/Pagination'
import { SAMPLE_NOTICES } from '../../../data/sampleBoard'

// 공지사항 목록 (/notice).
// 보기는 누구나 되고, 작성 · 수정 · 삭제는 관리자만 된다.
//
// 퍼블리싱만 된 화면이다. 가데이터를 그리고, 페이지 번호 색만 바뀐다.
// 공지 작성 버튼도 지금은 늘 보인다.
//
// TODO(공지 담당)
//   - 목록을 getNotices({ page }) 로 받아온다 (notices · totalPages 를 state 로)
//     고정 공지(pinned)가 위, 그 다음 최신순으로 서버가 정렬해서 준다
//   - 공지 작성 버튼을 관리자일 때만 보이게 한다 (user.is_admin)
//     로그인한 사람은 App.jsx 의 <Route> 에서 user={user} 로 받는다
//   - 로딩 · 오류 · 빈 목록 상태를 붙인다 (CODE_STYLE 3번)
export default function NoticePage() {
  const [page, setPage] = useState(1)

  const notices = SAMPLE_NOTICES
  const totalPages = 2 // 가데이터. 버튼 모양만 보여준다

  return (
    <div className="card">
      <div className="notice-page__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          공지사항
        </h2>

        <Link to="/notice/write" className="btn btn--sm">
          공지 작성
        </Link>
      </div>

      <p className="muted notice-page__desc">서비스 점검 · 업데이트 소식을 안내해드립니다.</p>

      <CommunityTabs />

      <ul className="notice-list">
        {notices.map((notice) => (
          <NoticeCard key={notice.id} notice={notice} />
        ))}
      </ul>

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
