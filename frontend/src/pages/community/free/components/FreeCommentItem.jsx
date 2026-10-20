import { useState } from 'react'
import { formatDateTime } from '../../../../utils/format'

const COMMENT_MAX = 1000 // 댓글 최대 글자 수

// 댓글 한 줄. 글 상세(FreeDetailPage)에서 댓글마다 하나씩 그린다.
// 댓글마다 "지금 수정 중인지" 를 따로 가지고 있어야 해서 컴포넌트로 뺐다.
// 부모가 댓글 다섯 개의 수정 상태를 한꺼번에 들고 있으려면 배열로 관리해야
// 하는데, 컴포넌트로 나누면 각자 useState 하나씩 가지면 끝난다.
//   comment  : 댓글 { id, nickname, content, created_at, is_mine }
//   onEdit   : 수정 저장을 누르면 부를 함수 (comment, 새 내용)
//   onDelete : 삭제를 누르면 부를 함수 (comment)
//
// 로그인한 사람(user)을 받지 않는다. comment.is_mine 이 이미 "이 댓글을 내가
// 썼는지" 를 담고 있어서 여기서 회원 번호를 견줄 일이 없다.
// 이 값은 글의 is_mine 과 다른 값이다 — 남의 글에 달린 내 댓글은 글 쪽이
// false 라도 여기는 true 고, 내 글에 달린 남의 댓글은 그 반대다.
// 그래서 부모가 post.is_mine 을 내려주면 안 되고, 댓글이 가진 값을 봐야 한다.
//
// 서버로 보내는 일은 하지 않는다. 입력칸만 들고 있다가 부모의 onEdit · onDelete
// 를 부른다 — 댓글 목록을 들고 있는 쪽이 부모라서, 보내고 목록을 갈아끼우는
// 일은 거기서 한 번에 한다.
export default function FreeCommentItem({ comment, onEdit, onDelete }) {
  const [editing, setEditing] = useState(false) // 수정 중인지
  const [text, setText] = useState(comment.content) // 수정 입력칸 글자

  // 수정 저장. 실제 요청은 부모(onEdit)가 보낸다.
  // 결과를 기다리지 않고 입력칸을 닫는다 — 실패하면 부모가 화면 위쪽에
  // 오류 문장을 띄우고, 댓글 내용은 바뀌지 않은 채로 남는다
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

        {/* 이 댓글을 내가 썼을 때만 수정 · 삭제.
            글쓴이인지와는 상관이 없다 — 남의 글이어도 내 댓글이면 보이고,
            내 글이어도 남이 단 댓글이면 안 보인다 */}
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
