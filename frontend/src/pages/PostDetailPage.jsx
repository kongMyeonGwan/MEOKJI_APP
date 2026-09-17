import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import RecipeView from '../components/recipe/RecipeView'
import { useAuth } from '../contexts/AuthContext'
import { useProfile } from '../contexts/ProfileContext'
import {
  addComment,
  countPostView,
  getPost,
  removeComment,
  removePost,
  removePostFile,
  setPostLike,
  updateComment,
  updatePost,
  uploadPostFile,
} from '../api/posts'
import PostAttachment from '../components/community/PostAttachment'
import { parseApiError } from '../utils/apiError'
import { formatDateTime } from '../utils/format'
import '../styles/pages/PostDetailPage.css'

// 게시글 상세 + 좋아요 + 댓글.
//
// 로그인 안 해도 볼 수 있다.
// 좋아요, 댓글, 수정, 삭제만 로그인이 필요하고
// 수정이랑 삭제는 내가 쓴 것만 된다. 서버가 다시 확인한다.
//
// 파일 안에 컴포넌트가 두 개인 이유는 아래 PostDetail 에 key 를 주려고 그렇다.
// key 가 바뀌면 리액트가 다른 컴포넌트로 보고 처음부터 새로 만든다.
// 이게 없으면 1번 글에서 2번 글로 갔을 때 좋아요랑 댓글이 1번 글 값으로 남는다.
export default function PostDetailPage() {
  const { postId } = useParams() // 주소의 /community/:postId 부분
  return <PostDetail key={postId} postId={postId} />
}

function PostDetail({ postId }) {
  const { isAuthenticated } = useAuth()
  const { reload } = useProfile()
  const navigate = useNavigate()

  // 게시글
  const [post, setPost] = useState(null)
  const [comments, setComments] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  // 좋아요
  const [liked, setLiked] = useState(false) // 내가 눌렀는지
  const [likeCount, setLikeCount] = useState(0) // 전체 개수
  const [likeBusy, setLikeBusy] = useState(false) // 요청 중이면 버튼 잠금

  // 댓글 작성
  const [draft, setDraft] = useState('') // 입력칸에 쓰는 중인 글
  const [commentBusy, setCommentBusy] = useState(false)
  const [actionError, setActionError] = useState('')

  // 글 수정
  const [editing, setEditing] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editContent, setEditContent] = useState('')
  const [editBusy, setEditBusy] = useState(false)

  // 사진
  const [fileBusy, setFileBusy] = useState(false)
  const fileInput = useRef(null)

  // 글쓰기 화면에서 사진 업로드만 실패했을 때 들려 보낸 메시지.
  // 글은 등록됐으므로 여기서 "다시 올려보세요" 라고 알려주는 편이 낫다.
  const location = useLocation()

  let firstFileError = ''
  if (location.state && location.state.fileError) {
    firstFileError = location.state.fileError
  }

  const [fileError, setFileError] = useState(firstFileError)

  // 화면이 처음 열릴 때 게시글을 불러오고, 조회수를 한 번 올린다.
  //
  // 조회수를 GET 에서 같이 올리지 않는 이유는, GET 은 몇 번을 보내도 결과가
  // 같아야 하기 때문이다. 그래서 서버에 따로 엔드포인트를 뒀고 화면이 부른다.
  useEffect(() => {
    getPost(postId)
      .then(function (data) {
        setPost(data)
        // 서버가 준 값을 각각의 state 에 나눠 담는다.
        // 좋아요랑 댓글은 이 화면에서 바뀌니까 따로 들고 있어야 한다.
        setComments(data.comments)
        setLiked(data.liked)
        setLikeCount(data.like_count)
        setEditTitle(data.title)
        setEditContent(data.content)
      })
      .catch(function (err) {
        setLoadError(parseApiError(err).message)
      })
      .finally(function () {
        setLoading(false)
      })

    // 조회수는 실패해도 무시한다. 숫자 하나 때문에 에러를 띄울 이유가 없다.
    countPostView(postId).catch(function () {})
  }, [postId])

  if (loading) {
    return (
      <div className="card">
        <Loading message="게시글을 불러오는 중..." />
      </div>
    )
  }

  if (loadError || !post) {
    let errorText = loadError
    if (!errorText) {
      errorText = '게시글을 찾을 수 없습니다.'
    }

    return (
      <div className="card">
        <p className="empty">
          {errorText}
          <br />
          <Link to="/community" className="post__link">
            목록으로 →
          </Link>
        </p>
      </div>
    )
  }

  // 하트를 누르면 실행된다.
  //
  // 서버 응답을 기다리지 않고 화면부터 먼저 바꾼다.
  // 하트는 바로 반응해야 눌린 느낌이 나는데 서버를 기다리면 잠깐 멍하다.
  // 대신 실패하면 아래 catch 에서 되돌린다.
  async function onToggleLike() {
    // 실패했을 때 되돌릴 수 있도록 지금 값을 먼저 적어둔다
    const previousLiked = liked
    const previousCount = likeCount

    const nextLiked = !liked // 켜져 있으면 끄고, 꺼져 있으면 켠다

    // 1) 화면부터 먼저 바꾼다
    setLiked(nextLiked)
    if (nextLiked) {
      setLikeCount(previousCount + 1)
    } else {
      setLikeCount(previousCount - 1)
    }

    setActionError('')
    setLikeBusy(true) // 응답이 올 때까지 버튼을 잠근다

    try {
      // 2) 서버에 알린다
      const result = await setPostLike(post.id, nextLiked)

      // 3) 서버가 직접 세어준 값으로 덮어쓴다.
      //    내가 누르는 사이에 남이 누른 것까지 반영된다.
      setLiked(result.liked)
      setLikeCount(result.like_count)
    } catch (err) {
      // 실패했으니 1)에서 바꾼 화면을 눌렀던 순간의 값으로 되돌린다
      setLiked(previousLiked)
      setLikeCount(previousCount)
      setActionError(parseApiError(err).message)
    } finally {
      setLikeBusy(false)
    }
  }

  // 댓글 등록 버튼을 누르면 실행된다
  async function onSubmitComment(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    const content = draft.trim()
    if (!content) {
      return
    }

    setActionError('')
    setCommentBusy(true)

    try {
      const created = await addComment(post.id, content)

      // 서버가 준 댓글을 목록 맨 뒤에 붙인다.
      // 댓글은 쓴 순서대로 보여줘서 맨 뒤가 맞다.
      setComments(comments.concat([created]))

      setDraft('') // 입력칸을 비운다
      reload() // 알림 개수를 다시 맞춘다
    } catch (err) {
      setActionError(parseApiError(err).message)
    } finally {
      setCommentBusy(false)
    }
  }

  // 댓글을 고친다. 서버가 준 값으로 그 자리만 갈아 끼운다.
  async function onEditComment(comment, content) {
    const updated = await updateComment(post.id, comment.id, content)

    setComments(
      comments.map(function (item) {
        if (item.id === comment.id) {
          return updated
        }
        return item
      }),
    )
  }

  // 댓글을 지운다
  async function onRemoveComment(comment) {
    const ok = window.confirm('댓글을 지울까요?')
    if (!ok) {
      return
    }

    setActionError('')

    try {
      await removeComment(post.id, comment.id)

      setComments(
        comments.filter(function (item) {
          return item.id !== comment.id
        }),
      )
      reload()
    } catch (err) {
      setActionError(parseApiError(err).message)
    }
  }

  // 글 수정 저장
  async function onSaveEdit(e) {
    e.preventDefault()

    setEditBusy(true)
    setActionError('')

    try {
      const updated = await updatePost(post.id, {
        title: editTitle.trim(),
        content: editContent.trim(),
      })
      setPost(updated)
      setEditing(false)
    } catch (err) {
      const parsed = parseApiError(err)

      let text = parsed.fieldErrors.title
      if (!text) {
        text = parsed.fieldErrors.content
      }
      if (!text) {
        text = parsed.message
      }

      setActionError(text)
    } finally {
      setEditBusy(false)
    }
  }

  // 글 삭제. 지우고 나면 볼 화면이 없어서 목록으로 보낸다.
  async function onRemovePost() {
    const ok = window.confirm(
      '이 글을 지울까요?\n달린 댓글도 함께 보이지 않게 됩니다.',
    )
    if (!ok) {
      return
    }

    setActionError('')

    try {
      await removePost(post.id)
      reload()
      navigate('/community')
    } catch (err) {
      setActionError(parseApiError(err).message)
    }
  }

  // 사진 붙이기 / 갈아 끼우기.
  //
  // 서버가 바뀐 게시글을 통째로 돌려줘서 그걸 그대로 쓴다.
  // 화면에서 file 만 직접 만들어 넣으면 서버가 계산한 크기 표시나
  // 내려받기 주소가 화면이랑 어긋날 수 있다.
  async function onPickFile(e) {
    let picked = null
    if (e.target.files && e.target.files.length > 0) {
      picked = e.target.files[0]
    }

    if (!picked) {
      return
    }

    setFileError('')
    setFileBusy(true)

    try {
      const updated = await uploadPostFile(post.id, picked)
      setPost(updated)
    } catch (err) {
      setFileError(parseApiError(err).message)
    } finally {
      setFileBusy(false)

      // 같은 파일을 또 골라도 onChange 가 돌게 입력칸을 비운다.
      // 안 비우면 방금 실패한 파일을 다시 고를 때 아무 일도 안 일어난다.
      if (fileInput.current) {
        fileInput.current.value = ''
      }
    }
  }

  // 사진 떼기. 서버가 DB 를 정리하고 디스크에서도 지운다.
  async function onRemoveFile() {
    const ok = window.confirm('첨부한 사진을 지울까요?')
    if (!ok) {
      return
    }

    setFileError('')
    setFileBusy(true)

    try {
      await removePostFile(post.id)

      // 사진만 빼고 나머지는 그대로 둔다
      const next = Object.assign({}, post)
      next.file = null
      setPost(next)
    } catch (err) {
      setFileError(parseApiError(err).message)
    } finally {
      setFileBusy(false)
    }
  }

  let editSaveLabel = '저장'
  if (editBusy) {
    editSaveLabel = '저장 중…'
  }

  let commentSubmitLabel = '등록'
  if (commentBusy) {
    commentSubmitLabel = '등록 중...'
  }

  let likeClass = 'post__like'
  if (liked) {
    likeClass = 'post__like is-on'
  }

  let likeTitle = '로그인이 필요합니다'
  if (isAuthenticated) {
    likeTitle = ''
  }

  // 글 수정을 취소했을 때 원래 값으로 되돌린다
  function cancelEdit() {
    setEditTitle(post.title)
    setEditContent(post.content)
    setEditing(false)
  }

  return (
    <div className="post">
      <p className="muted post__back">
        <Link to="/community">← 커뮤니티</Link>
      </p>

      <article className="card">
        {editing ? (
          <form className="post__edit" onSubmit={onSaveEdit}>
            <label className="post__edit-field">
              <span>제목</span>
              <input
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                maxLength={200}
                required
              />
            </label>

            <label className="post__edit-field">
              <span>내용</span>
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                rows={12}
                required
              />
            </label>

            <div className="post__edit-actions">
              <button type="submit" className="btn btn--sm" disabled={editBusy}>
                {editSaveLabel}
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={cancelEdit}
              >
                취소
              </button>
            </div>
          </form>
        ) : (
          <>
            <h2 className="post__title">{post.title}</h2>

            <div className="post__meta">
              <span>{post.nickname}</span>
              <span className="muted">{formatDateTime(post.created_at)}</span>
              <span className="muted">조회 {post.view_count}</span>
              {post.food_name && <span className="tag">{post.food_name}</span>}
            </div>

            {/* 줄바꿈을 살리기 위해 white-space: pre-wrap 을 쓴다 */}
            <div className="post__content">{post.content}</div>

            {/* 본문 바로 아래, 레시피보다 위에 둔다.
                "이렇게 만들어 먹었다" 는 사진이 후기의 본론이고
                레시피는 그 근거로 뒤따라오는 순서다. */}
            <PostAttachment
              file={post.file}
              isMine={post.is_mine}
              busy={fileBusy}
              error={fileError}
              inputRef={fileInput}
              onPick={onPickFile}
              onRemove={onRemoveFile}
            />
          </>
        )}

        {/* 글쓴이가 확정했을 때의 레시피. 서버가 원본 기록에서 가져와 준다 */}
        {post.recipe_json && (
          <div className="post__recipe">
            <RecipeView
              recipe={post.recipe_json}
              ingredients={post.ingredients_json}
            />
          </div>
        )}

        <div className="post__actions">
          <button
            type="button"
            className={likeClass}
            onClick={onToggleLike}
            disabled={!isAuthenticated || likeBusy}
            title={likeTitle}
          >
            ♥ {likeCount}
          </button>
          {!isAuthenticated && (
            <span className="muted post__hint">
              <Link to="/login" className="post__link">
                로그인
              </Link>{" "}
              후 좋아요를 누를 수 있습니다
            </span>
          )}

          {/* 내 글에만 나오는 버튼. 서버도 남의 글이면 404로 막는다 */}
          {post.is_mine && !editing && (
            <span className="post__owner-actions">
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={() => setEditing(true)}
              >
                수정
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm post__danger"
                onClick={onRemovePost}
              >
                삭제
              </button>
            </span>
          )}
        </div>

        <ErrorMessage message={actionError} />
      </article>

      <section className="card">
        <h3 className="section-title">댓글 {comments.length}</h3>

        {comments.length === 0 && <p className="empty">첫 댓글을 남겨보세요.</p>}

        {comments.length > 0 && (
          <ul className="post__comments divided">
            {comments.map(function (comment) {
              return (
                <Comment
                  key={comment.id}
                  comment={comment}
                  onEdit={onEditComment}
                  onRemove={onRemoveComment}
                />
              )
            })}
          </ul>
        )}

        {isAuthenticated ? (
          <form className="post__form" onSubmit={onSubmitComment}>
            <textarea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="댓글을 입력하세요"
              maxLength={1000}
              rows={3}
            />
            <div className="post__form-foot">
              <span className="muted">{draft.length} / 1000</span>
              <button
                type="submit"
                className="btn btn--sm"
                disabled={!draft.trim() || commentBusy}
              >
                {commentSubmitLabel}
              </button>
            </div>
          </form>
        ) : (
          <p className="muted post__login-ask">
            <Link to="/login" className="post__link">
              로그인
            </Link>{" "}
            후 댓글을 작성할 수 있습니다.
          </p>
        )}
      </section>
    </div>
  )
}

// 댓글 한 줄.
//
// 따로 컴포넌트로 뺀 이유는 "지금 고치는 중인가" 를 댓글마다 따로
// 들고 있어야 해서다. 부모가 들고 있으면 어느 댓글을 고치는 중인지
// id 로 비교하는 코드가 여기저기 생긴다.
function Comment({ comment, onEdit, onRemove }) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(comment.content)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function onSubmit(e) {
    e.preventDefault()

    const content = draft.trim()
    if (!content) {
      return
    }

    setBusy(true)
    setError('')

    try {
      await onEdit(comment, content)
      setEditing(false)
    } catch (err) {
      setError(parseApiError(err).message)
    } finally {
      setBusy(false)
    }
  }

  let saveLabel = '저장'
  if (busy) {
    saveLabel = '저장 중…'
  }

  // 고치던 내용을 버리고 원래대로 되돌린다
  function cancel() {
    setDraft(comment.content)
    setEditing(false)
  }

  return (
    <li>
      <div className="post__comment-head">
        <strong>{comment.nickname}</strong>
        <span className="muted">{formatDateTime(comment.created_at)}</span>

        {/* 내 댓글에만 나오는 버튼. 서버도 남의 댓글이면 404로 막는다 */}
        {comment.is_mine && !editing && (
          <span className="post__comment-actions">
            <button type="button" onClick={() => setEditing(true)}>
              수정
            </button>
            <button
              type="button"
              className="post__danger"
              onClick={() => onRemove(comment)}
            >
              삭제
            </button>
          </span>
        )}
      </div>

      {editing ? (
        <form className="post__form" onSubmit={onSubmit}>
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            maxLength={1000}
            rows={3}
          />
          <div className="post__form-foot">
            <span className="muted">{draft.length} / 1000</span>
            <span className="post__edit-actions">
              <button type="submit" className="btn btn--sm" disabled={busy}>
                {saveLabel}
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={cancel}
              >
                취소
              </button>
            </span>
          </div>
          <ErrorMessage message={error} />
        </form>
      ) : (
        <p className="post__comment-body">{comment.content}</p>
      )}
    </li>
  )
}
