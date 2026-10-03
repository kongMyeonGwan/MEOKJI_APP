import { useState } from 'react'
import { formatDateTime } from '../../../../utils/format'

const COMMENT_MAX = 1000 // 댓글 최대 글자 수

// 댓글 한 줄. 글 상세(FreeDetailPage)에서 댓글마다 하나씩 그린다.
// 댓글마다 "지금 수정 중인지" 를 따로 가지고 있어야 해서 컴포넌트로 뺐다.
//   comment  : 댓글 { id, nickname, content, created_at, is_mine }
//   onEdit   : 수정 저장을 누르면 부를 함수 (comment, 새 내용)
//   onDelete : 삭제를 누르면 부를 함수 (comment)
export default function FreeCommentItem({ comment, onEdit, onDelete }) {
  const [editing, setEditing] = useState(false) // 수정 중인지
  const [text, setText] = useState(comment.content) // 수정 입력칸 글자

  const handleSave = () => {
    if (!text.trim()) {
      return
    }
    onEdit(comment, text.trim())
    setEditing(false)
  }

  // 수정 취소: 원래 내용으로 되돌린다
  const handleCancel = () => {
    setText(comment.content)
    setEditing(false)
  }

  return (
    <li>
      <div className="free-detail__comment-head">
        <strong>{comment.nickname}</strong>
        <span className="muted">{formatDateTime(comment.created_at)}</span>

        {/* 내 댓글이면 수정/삭제 버튼 */}
        {comment.is_mine && !editing && (
          <span className="free-detail__comment-actions">
            <button type="button" onClick={() => setEditing(true)}>
              수정
            </button>
            <button type="button" className="free-detail__danger" onClick={() => onDelete(comment)}>
              삭제
            </button>
          </span>
        )}
      </div>

      {editing ? (
        <div className="free-detail__form">
          <textarea value={text} onChange={(e) => setText(e.target.value)} maxLength={COMMENT_MAX} rows={3} />
          <div className="free-detail__form-foot">
            {/* 거의 다 차면 빨간 글씨로 알려준다 */}
            <span className={text.length > COMMENT_MAX - 100 ? 'free-detail__count-warn' : 'muted'}>
              {text.length} / {COMMENT_MAX}
            </span>
            <span className="free-detail__edit-actions">
              <button type="button" className="btn btn--sm" onClick={handleSave} disabled={!text.trim()}>
                저장
              </button>
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleCancel}>
                취소
              </button>
            </span>
          </div>
        </div>
      ) : (
        <p className="free-detail__comment-body">{comment.content}</p>
      )}
    </li>
  )
}
