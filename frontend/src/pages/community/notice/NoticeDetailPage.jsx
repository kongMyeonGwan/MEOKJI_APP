import { useState } from 'react'
import { Link } from 'react-router-dom'
import NoticeForm from './components/NoticeForm'
import { SAMPLE_NOTICE_DETAIL } from '../../../data/sampleBoard'
import { formatDate } from '../../../utils/format'

// 공지 상세 (/notice/1).
// 보기는 누구나 되고, 수정 · 삭제는 관리자만 된다.
//
// 퍼블리싱만 된 화면이다. 가데이터를 그려서 주소의 번호와 상관없이 같은 공지가 나온다.
// 수정 폼 열고 닫기만 동작하고, 수정 · 삭제 버튼은 아직 아무 일도 하지 않는다.
//
// TODO(공지 담당)
//   - useParams 로 :noticeId 를 꺼내 getNotice(noticeId) 로 받아온다
//   - 조회수를 countNoticeView(noticeId) 로 올린다 (화면에 들어올 때 한 번)
//   - 수정 폼 저장은 updateNotice(noticeId, form), 삭제는 deleteNotice(noticeId) 다
//     삭제한 뒤에는 목록으로 보낸다 — navigate('/notice', { replace: true })
//   - 수정 · 삭제 버튼을 관리자일 때만 보이게 한다 (user.is_admin)
export default function NoticeDetailPage() {
  const notice = SAMPLE_NOTICE_DETAIL

  // 수정 폼을 열었는지 (화면 전환만 한다)
  const [editing, setEditing] = useState(false)

  return (
    <div className="notice-detail">
      <p className="muted notice-detail__back">
        <Link to="/notice">← 공지사항</Link>
      </p>

      {editing ? (
        <div className="card">
          <NoticeForm notice={notice} onCancel={() => setEditing(false)} />
        </div>
      ) : (
        <article className="card">
          <h2 className="notice-detail__title">
            {notice.pinned && <span className="tag notice__pin">고정</span>}
            {notice.title}
          </h2>

          <div className="notice-detail__meta">
            <span>뭐먹지? 운영팀</span>
            <span className="muted">{formatDate(notice.created_at)}</span>
            <span className="muted">조회 {notice.view_count}</span>
          </div>

          <p className="notice__content notice-detail__content">{notice.content}</p>

          <div className="notice-detail__actions">
            <Link to="/notice" className="btn btn--ghost btn--sm">
              목록
            </Link>

            {/* 관리자에게만 보일 자리 */}
            <span className="notice-detail__admin">
              <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(true)}>
                수정
              </button>
              <button type="button" className="btn btn--ghost btn--sm notice-detail__danger">
                삭제
              </button>
            </span>
          </div>
        </article>
      )}
    </div>
  )
}
