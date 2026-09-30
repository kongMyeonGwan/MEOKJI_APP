import { useState } from 'react'
import { formatDateTime } from '../../utils/format'

// 댓글 한 줄. 게시글 상세(ReviewDetailPage)에서 댓글마다 하나씩 그린다.
// 댓글마다 "지금 수정 중인지" 를 따로 가지고 있어야 해서 컴포넌트로 뺐다.
//   comment  : 댓글 { id, nickname, content, created_at, is_mine }
//   onEdit   : 수정 저장을 누르면 부를 함수 (comment, 새 내용)
//   onDelete : 삭제를 누르면 부를 함수 (comment)
export default function CommentItem({ comment, onEdit, onDelete }) {
  const [editing, setEditing] = useState(false) // 수정 중인지
  const [text, setText] = useState(comment.content) // 수정 입력칸 글자

  const handleSubmit = (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    if (text.trim() === '') {
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
      <div className="review__comment-head">
        <strong>{comment.nickname}</strong>
        <span className="muted">{formatDateTime(comment.created_at)}</span>

        {/* 내 댓글이면 수정/삭제 버튼 */}
        {comment.is_mine && !editing && (
          <span className="review__comment-actions">
            <button type="button" onClick={() => setEditing(true)}>
              수정
            </button>
            <button type="button" className="review__danger" onClick={() => onDelete(comment)}>
              삭제
            </button>
          </span>
        )}
      </div>

      {editing ? (
        <form className="review__form" onSubmit={handleSubmit}>
          <textarea value={text} onChange={(e) => setText(e.target.value)} maxLength={1000} rows={3} />
          <div className="review__form-foot">
            <span className="muted">{text.length} / 1000</span>
            <span className="review__edit-actions">
              <button type="submit" className="btn btn--sm">
                저장
              </button>
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleCancel}>
                취소
              </button>
            </span>
          </div>
        </form>
      ) : (
        <p className="review__comment-body">{comment.content}</p>
      )}
    </li>
  )
}
