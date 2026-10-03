import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import FreeAttachment from './components/FreeAttachment'
import FreeCommentItem from './components/FreeCommentItem'
import Modal from '../../../components/common/Modal'
import Loading from '../../../components/common/Loading'
import ErrorMessage from '../../../components/common/ErrorMessage'
import { formatDateTime } from '../../../utils/format'
import {
  addFreeComment,
  countFreePostView,
  getFreePost,
  removeFreeComment,
  removeFreePost,
  removeFreePostFile,
  updateFreeComment,
  updateFreePost,
  uploadFreePostFile,
} from '../../../api/freePosts'

const COMMENT_MAX = 1000 // 댓글 최대 글자 수

// 자유게시판 글 상세 (/free/1).
// 보는 건 로그인 없이 되고, 수정 · 삭제는 내 글만 된다. 댓글은 로그인해야 쓴다.
// 좋아요는 없다 — 표를 만들지 않았다 (db/09_add_free_posts.sql 참고).
export default function FreeDetailPage({ user }) {
  const { postId } = useParams() // 주소의 :postId 부분

  const [post, setPost] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('') // 글을 못 불러왔을 때
  const [actionError, setActionError] = useState('') // 수정 · 삭제 · 댓글이 실패했을 때

  // 글 수정. 입력칸이 둘뿐이고 값을 서버에서 받아온 뒤에 채워야 해서
  // useForm 대신 useState 를 쓴다 (useForm 은 처음 값을 한 번만 받는다)
  const [editing, setEditing] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editContent, setEditContent] = useState('')

  // 댓글 입력칸
  const [commentText, setCommentText] = useState('')

  // 삭제 확인 창 (window.confirm 대신 Modal 을 쓴다)
  const [confirmOpen, setConfirmOpen] = useState(false)

  // 사진 올리는 중인지
  const [fileBusy, setFileBusy] = useState(false)
  const [fileError, setFileError] = useState('')

  const navigate = useNavigate()

  // 서버가 보낸 오류 문장을 꺼낸다. 화면 여러 군데에서 같은 방식으로 쓴다
  const messageOf = (err) => {
    if (err.response) {
      // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '댓글을 입력해주세요' }]
      const detail = err.response.data.detail
      if (Array.isArray(detail)) {
        return detail[0].msg
      }
      return detail || '요청을 처리하지 못했습니다.'
    }
    if (err.request) {
      // 백엔드를 안 켰거나 30초가 지난 경우
      return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.'
    }
    return err.message
  }

  // 화면에 들어올 때 한 번: 글을 받아오고 조회수를 올린다
  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const data = await getFreePost(postId)
        setPost(data)
        setError('')

        // 조회수는 여기서 따로 올린다. 목록을 열 때마다 오르면 숫자가 의미를 잃는다.
        // 실패해도 글은 이미 보이니 화면을 막지 않는다.
        try {
          const count = await countFreePostView(postId)
          setPost({ ...data, view_count: count })
        } catch (ignore) {
          // 조회수만 못 올린 것이라 넘어간다
        }
      } catch (err) {
        setError(messageOf(err))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [postId])

  // 수정 폼 열기. 지금 값으로 입력칸을 채운다
  const handleEditOpen = () => {
    setEditTitle(post.title)
    setEditContent(post.content)
    setActionError('')
    setEditing(true)
  }

  // 수정 저장
  const handleEditSave = async (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    if (!editTitle.trim()) {
      setActionError('제목을 입력해주세요')
      return
    }
    if (!editContent.trim()) {
      setActionError('내용을 입력해주세요')
      return
    }

    setActionError('')
    try {
      const data = await updateFreePost(postId, {
        title: editTitle.trim(),
        content: editContent,
      })
      setPost(data)
      setEditing(false)
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 글 삭제. replace 를 쓰면 뒤로 가기를 눌러도 지워진 글로 안 돌아온다
  const handleDelete = async () => {
    setConfirmOpen(false)
    try {
      await removeFreePost(postId)
      navigate('/free', { replace: true })
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 댓글 등록
  const handleCommentSubmit = async (e) => {
    e.preventDefault()

    if (!commentText.trim()) {
      return
    }

    setActionError('')
    try {
      const added = await addFreeComment(postId, commentText.trim())
      setPost({ ...post, comments: [...post.comments, added] })
      setCommentText('')
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 댓글 수정
  const handleCommentEdit = async (comment, content) => {
    setActionError('')
    try {
      const saved = await updateFreeComment(postId, comment.id, content)
      // 바뀐 댓글만 갈아끼운다
      const next = post.comments.map((item) => (item.id === comment.id ? saved : item))
      setPost({ ...post, comments: next })
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 댓글 삭제
  const handleCommentDelete = async (comment) => {
    setActionError('')
    try {
      await removeFreeComment(postId, comment.id)
      setPost({ ...post, comments: post.comments.filter((item) => item.id !== comment.id) })
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 사진 붙이기 · 바꾸기
  const handlePickFile = async (e) => {
    const picked = e.target.files[0]
    e.target.value = '' // 같은 파일을 다시 골라도 onChange 가 불리게 비운다

    if (!picked) {
      return
    }
    if (picked.size > 5 * 1024 * 1024) {
      setFileError('5MB 이하 사진만 올릴 수 있어요')
      return
    }

    setFileError('')
    setFileBusy(true)
    try {
      const file = await uploadFreePostFile(postId, picked)
      setPost({ ...post, file: file })
    } catch (err) {
      setFileError(messageOf(err))
    } finally {
      setFileBusy(false)
    }
  }

  // 사진 지우기
  const handleRemoveFile = async () => {
    setFileError('')
    setFileBusy(true)
    try {
      await removeFreePostFile(postId)
      setPost({ ...post, file: null })
    } catch (err) {
      setFileError(messageOf(err))
    } finally {
      setFileBusy(false)
    }
  }

  if (loading) {
    return <Loading message="글을 불러오는 중..." />
  }

  if (error) {
    return (
      <div className="free-detail">
        <p className="muted free-detail__back">
          <Link to="/free">← 자유게시판</Link>
        </p>
        <div className="card">
          <ErrorMessage message={error} />
        </div>
      </div>
    )
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
              <input
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                maxLength={200}
              />
            </label>
            <label className="free-detail__edit-field">
              <span>내용</span>
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                rows={12}
                maxLength={2000}
              />
            </label>
            <div className="free-detail__edit-actions">
              <button type="submit" className="btn btn--sm">
                저장
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={() => setEditing(false)}
              >
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
              isMine={post.is_mine}
              busy={fileBusy}
              error={fileError}
              onPick={handlePickFile}
              onRemove={handleRemoveFile}
            />
          </>
        )}

        <ErrorMessage message={actionError} />

        <div className="free-detail__actions">
          <Link to="/free" className="btn btn--ghost btn--sm">
            목록
          </Link>

          {/* 내 글이면 수정 · 삭제 버튼 */}
          {post.is_mine && !editing && (
            <span className="free-detail__owner-actions">
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleEditOpen}>
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

      <section className="card">
        <h3 className="section-title">댓글 {post.comments.length}</h3>

        {post.comments.length === 0 && <p className="empty">첫 댓글을 남겨보세요.</p>}

        {post.comments.length > 0 && (
          <ul className="free-detail__comments divided">
            {post.comments.map((comment) => (
              <FreeCommentItem
                key={comment.id}
                comment={comment}
                onEdit={handleCommentEdit}
                onDelete={handleCommentDelete}
              />
            ))}
          </ul>
        )}

        {user ? (
          <form className="free-detail__form" onSubmit={handleCommentSubmit}>
            <textarea
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="댓글을 입력하세요"
              maxLength={COMMENT_MAX}
              rows={3}
            />
            <div className="free-detail__form-foot">
              {/* 거의 다 차면 빨간 글씨로 알려준다 */}
              <span className={commentText.length > COMMENT_MAX - 100 ? 'free-detail__count-warn' : 'muted'}>
                {commentText.length} / {COMMENT_MAX}
              </span>
              <button type="submit" className="btn btn--sm" disabled={!commentText.trim()}>
                등록
              </button>
            </div>
          </form>
        ) : (
          <p className="muted free-detail__login-ask">
            <Link to="/login" className="free-detail__link">
              로그인
            </Link>{' '}
            후 댓글을 작성할 수 있습니다.
          </p>
        )}
      </section>

      {confirmOpen && (
        <Modal onClose={() => setConfirmOpen(false)}>
          <h3 className="modal__title">이 글을 지울까요?</h3>
          <p className="muted">달린 댓글과 사진도 같이 보이지 않게 됩니다.</p>
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
