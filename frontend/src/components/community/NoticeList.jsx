import { useState } from 'react'
import { formatDate } from '../../utils/format'
import '../../styles/components/community/NoticeList.css'

// 공지사항 목록. 제목을 누르면 그 자리에서 본문이 펼쳐진다.
// canManage 가 true 면(관리자) 수정/삭제 버튼이 보인다.
//
// TODO(게시판 담당): 공지를 펼칠 때 countNoticeView(notice.id) 로 조회수를 올린다.
export default function NoticeList({ notices, canManage, onEdit, onDelete }) {
  // 지금 펼쳐진 공지의 id. 한 번에 하나만 펼친다.
  const [openId, setOpenId] = useState(null)

  const toggleNotice = (notice) => {
    setOpenId(openId === notice.id ? null : notice.id)
  }

  const handleDelete = (notice) => {
    if (window.confirm(`'${notice.title}' 공지를 지울까요?`)) {
      onDelete(notice)
    }
  }

  if (notices.length === 0) {
    return <p className="empty">등록된 공지가 없습니다.</p>
  }

  return (
    <ul className="notice-list">
      {notices.map((notice) => (
        <li key={notice.id} className={notice.pinned ? 'notice notice--pinned' : 'notice'}>
          <button type="button" className="notice__head" onClick={() => toggleNotice(notice)}>
            {notice.pinned && (
              <span className="notice__badges">
                <span className="tag notice__pin">고정</span>
              </span>
            )}
            <span className="notice__title">{notice.title}</span>
            <span className="muted notice__meta">
              {formatDate(notice.created_at)} · 조회 {notice.view_count}
            </span>
            <span className={openId === notice.id ? 'notice__caret is-open' : 'notice__caret'}>⌄</span>
          </button>

          {openId === notice.id && (
            <div className="notice__body">
              <p className="notice__content">{notice.content}</p>
              <p className="muted notice__writer">— 뭐먹지? 운영팀</p>

              {canManage && (
                <div className="notice__admin">
                  <button type="button" className="btn btn--ghost btn--sm" onClick={() => onEdit(notice)}>
                    수정
                  </button>
                  <button type="button" className="btn btn--ghost btn--sm" onClick={() => handleDelete(notice)}>
                    삭제
                  </button>
                </div>
              )}
            </div>
          )}
        </li>
      ))}
    </ul>
  )
}
