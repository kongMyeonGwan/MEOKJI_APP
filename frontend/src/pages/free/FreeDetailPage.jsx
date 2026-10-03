import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import FreeAttachment from './components/FreeAttachment'
import Modal from '../../components/common/Modal'
import useForm from '../../hooks/useForm'
import { SAMPLE_FREE_POST_DETAIL } from '../../data/sampleFree'
import { formatDateTime } from '../../utils/format'

// 자유게시판 글 상세 (/free/1).
// 보는 건 로그인 없이 되고, 수정 · 삭제는 내 글만 된다.
// 좋아요 · 댓글은 표를 만들지 않아서 없다 (db/09_add_free_posts.sql 참고).
//
// TODO(자유게시판 담당): 지금은 가데이터(SAMPLE_FREE_POST_DETAIL)를 그리는 퍼블리싱 상태라
// 주소의 번호와 상관없이 같은 글이 나온다.
//   1. api/freePosts.js 에 getFreePost · updateFreePost · removeFreePost ·
//      uploadFreePostFile · removeFreePostFile 를 만든다
//   2. post 를 useState 로 바꾸고 useEffect 에서 getFreePost(postId) 로 받아온다
//      (조회수는 서버가 상세 조회 때 1 올린다)
//   3. 아래 handle... 안의 alert 를 API 호출로 바꾼다
export default function FreeDetailPage({ user }) {
  const { postId } = useParams() // 주소의 :postId 부분 (서버에 붙일 때 쓴다)

  const post = SAMPLE_FREE_POST_DETAIL

  // 글 수정
  const [editing, setEditing] = useState(false)
  const { values, handleChange, reset } = useForm({ title: post.title, content: post.content })

  // 삭제 확인 창 (window.confirm 대신 Modal 을 쓴다)
  const [confirmOpen, setConfirmOpen] = useState(false)

  // 내 글인지. 로그인 안 했으면 남의 글로 본다.
  const isMine = user && post.is_mine

  // 수정 저장 — TODO: updateFreePost(post.id, { title: values.title, content: values.content })
  const handleEditSave = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    alert('자유게시판 기능은 아직 준비 중입니다.')
    setEditing(false)
  }

  // 수정 취소 (원래 값으로 되돌린다)
  const handleEditCancel = () => {
    reset()
    setEditing(false)
  }

  // 글 삭제 — TODO: removeFreePost(post.id) 후 navigate('/free', { replace: true })
  //   replace: 지워진 글 주소를 방문 기록에서 빼서, 뒤로 가기를 눌러도 다시 안 오게 한다
  const handleDelete = () => {
    setConfirmOpen(false)
    alert('자유게시판 기능은 아직 준비 중입니다.')
  }

  // 사진 붙이기 · 바꾸기 — TODO: uploadFreePostFile(post.id, e.target.files[0])
  const handlePickFile = (e) => {
    alert('사진 첨부는 아직 준비 중입니다.')
    e.target.value = '' // 같은 파일을 다시 골라도 onChange 가 불리게 비운다
  }

  // 사진 지우기 — TODO: removeFreePostFile(post.id)
  const handleRemoveFile = () => {
    alert('사진 첨부는 아직 준비 중입니다.')
  }

  return (
    <div className="free-detail">
      <p className="muted free-detail__back">
        <Link to="/free">← 자유게시판</Link>
      </p>

      <article className="card">
        {editing ? (
          <form className="free-detail__edit" onSubmit={handleEditSave}>
            <label className="free-detail__edit-field">
              <span>제목</span>
              <input type="text" name="title" value={values.title} onChange={handleChange} maxLength={200} />
            </label>
            <label className="free-detail__edit-field">
              <span>내용</span>
              <textarea name="content" value={values.content} onChange={handleChange} rows={12} />
            </label>
            <div className="free-detail__edit-actions">
              <button type="submit" className="btn btn--sm">
                저장
              </button>
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleEditCancel}>
                취소
              </button>
            </div>
          </form>
        ) : (
          <>
            <h2 className="free-detail__title">{post.title}</h2>

            <div className="free-detail__meta">
              <span>{post.nickname}</span>
              <span className="muted">{formatDateTime(post.created_at)}</span>
              <span className="muted">조회 {post.view_count}</span>
            </div>

            <div className="free-detail__content">{post.content}</div>

            <FreeAttachment
              file={post.file}
              isMine={isMine}
              onPick={handlePickFile}
              onRemove={handleRemoveFile}
            />
          </>
        )}

        <div className="free-detail__actions">
          <Link to="/free" className="btn btn--ghost btn--sm">
            목록
          </Link>

          {/* 내 글이면 수정 · 삭제 버튼 */}
          {isMine && !editing && (
            <span className="free-detail__owner-actions">
              <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(true)}>
                수정
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm free-detail__danger"
                onClick={() => setConfirmOpen(true)}
              >
                삭제
              </button>
            </span>
          )}
        </div>
      </article>

      {confirmOpen && (
        <Modal onClose={() => setConfirmOpen(false)}>
          <h3 className="modal__title">이 글을 지울까요?</h3>
          <p className="muted">지운 글은 목록에서 바로 사라집니다.</p>
          <div className="modal__actions">
            <button type="button" className="btn btn--ghost btn--sm" onClick={() => setConfirmOpen(false)}>
              취소
            </button>
            <button type="button" className="btn btn--sm" onClick={handleDelete}>
              삭제하기
            </button>
          </div>
        </Modal>
      )}
    </div>
  )
}
