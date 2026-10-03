import { useState } from 'react'
import { formatDateTime } from '../../../../utils/format'

const COMMENT_MAX = 1000 // 댓글 최대 글자 수

// 댓글 한 줄. 게시글 상세(ReviewDetailPage)에서 댓글마다 하나씩 그린다.
// 댓글마다 "지금 수정 중인지" 를 따로 가지고 있어야 해서 컴포넌트로 뺐다.
//
// 퍼블리싱만 된 조각이다. 수정 폼 열고 닫기만 동작한다.
//
// TODO(게시판 담당)
//   - 저장을 누르면 updateComment(reviewId, comment.id, 내용) 을 부른다
//   - 삭제를 누르면 removeComment(reviewId, comment.id) 를 부른다
//   - 빈 내용은 보내지 않고, 글자 수를 세어 1000자에 가까우면 알려준다
export default function CommentItem({ comment }) {
  const [editing, setEditing] = useState(false) // 수정 폼을 열었는지

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
            <button type="button" className="review__danger">
              삭제
            </button>
          </span>
        )}
      </div>

      {editing ? (
        <div className="review__form">
          <textarea defaultValue={comment.content} maxLength={COMMENT_MAX} rows={3} />
          <div className="review__form-foot">
            <span className="muted">0 / {COMMENT_MAX}</span>
            <span className="review__edit-actions">
              <button type="button" className="btn btn--sm">
                저장
              </button>
              <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(false)}>
                취소
              </button>
            </span>
          </div>
        </div>
      ) : (
        <p className="review__comment-body">{comment.content}</p>
      )}
    </li>
  )
}
