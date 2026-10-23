import { Link } from 'react-router-dom'
import { formatDate } from '../../../../utils/format'

// 공지 목록의 한 줄. 제목을 누르면 상세 화면으로 간다.
// 고정 공지(pinned)는 배경이 옅게 깔리고 '고정' 배지가 붙는다.
export default function NoticeCard({ notice }) {
  return (
    <li className={notice.pinned ? 'notice notice--pinned' : 'notice'}>
      <Link to={`/notice/${notice.id}`} className="notice__head">
        {notice.pinned && (
          <span className="notice__badges">
            <span className="tag notice__pin">고정</span>
          </span>
        )}
        <span className="notice__title">{notice.title}</span>
        <span className="muted notice__meta">
          {formatDate(notice.createdAt)} · 조회 {notice.viewCount}
        </span>
        <span className="notice__caret">›</span>
      </Link>
    </li>
  )
}
