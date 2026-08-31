import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import { getHistories } from '../api/histories'
import { createPost } from '../api/posts'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/PostWritePage.css'

/**
 * 게시글 작성 (STEP 20).
 *
 * 글은 반드시 확정한 식사(History)에서 출발한다. 히스토리 없이 글만 쓰는 경로는 없다.
 * DB에서도 posts.meal_history_id 가 NOT NULL 이라 서버가 한 번 더 막는다.
 *
 * 고른 히스토리의 food_name / recipe_json / ingredients_json 이 게시글로 복사된다.
 * 참조가 아니라 복사라서, 이후 글을 수정해도 원본 스냅샷은 변하지 않는다.
 *
 * 이미 공유한 히스토리는 uq_posts_meal(UNIQUE)에 걸려 다시 고를 수 없으므로
 * 목록에서 미리 걸러낸다.
 */
export default function PostWritePage() {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [historyId, setHistoryId] = useState('')
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')

  const [histories, setHistories] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const navigate = useNavigate()

  useEffect(() => {
    let alive = true

    getHistories()
      .then((data) => {
        if (alive) setHistories(data)
      })
      .catch((err) => {
        if (alive) setLoadError(parseApiError(err).message)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [])

  // 이미 공유한 기록은 uq_posts_meal(UNIQUE)에 걸려 다시 올릴 수 없으므로 목록에서 뺀다.
  const sharableHistories = histories.filter((h) => !h.shared_post_id)

  const onSubmit = async (e) => {
    e.preventDefault()

    const next = {}
    if (!title.trim()) next.title = '제목을 입력해주세요'
    if (!historyId) next.historyId = '공유할 내 기록을 선택해주세요'
    if (!content.trim()) next.content = '내용을 입력해주세요'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSubmitError('')
    setSubmitting(true)
    try {
      // 레시피/재료는 보내지 않는다. 서버가 원본 기록에서 직접 복사한다.
      const created = await createPost({
        mealHistoryId: Number(historyId),
        title: title.trim(),
        content: content.trim(),
      })
      // 목록이 아니라 방금 쓴 글로 보낸다. 등록된 모습을 바로 확인할 수 있다.
      navigate(`/community/${created.id}`)
    } catch (err) {
      const { fieldErrors, message } = parseApiError(err)
      // 서버의 422는 필드별로 쪼개 각 입력칸 아래에 붙인다.
      setErrors({
        title: fieldErrors.title,
        content: fieldErrors.content,
        historyId: fieldErrors.meal_history_id,
      })
      setSubmitError(message)
      setSubmitting(false)
    }
  }

  return (
    <div className="card write">
      <div className="write__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          글쓰기
        </h2>
        <Link to="/community" className="muted write__cancel">
          취소
        </Link>
      </div>

      {loading ? (
        <Loading message="내 기록을 불러오는 중..." />
      ) : loadError ? (
        <ErrorMessage message={loadError} />
      ) : sharableHistories.length === 0 ? (
        <p className="muted write__help">
          공유할 수 있는 기록이 없습니다. 식사를 확정하면 이곳에서 글을 쓸 수 있어요.{' '}
          <Link to="/history">내 기록 보기</Link>
        </p>
      ) : (
        <form onSubmit={onSubmit} noValidate>
          <label className="write__field">
            <span>제목</span>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="어떤 이야기인가요?"
              maxLength={200}
              autoFocus
            />
            <ErrorMessage message={errors.title} />
          </label>

          <label className="write__field">
            <span>공유할 내 기록</span>
            <select value={historyId} onChange={(e) => setHistoryId(e.target.value)}>
              <option value="">기록을 선택해주세요</option>
              {sharableHistories.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.food_name} — {h.recipe_name}
                </option>
              ))}
            </select>
            <ErrorMessage message={errors.historyId} />
            <p className="muted write__help">
              선택한 기록의 레시피와 재료가 게시글에 함께 표시됩니다.
            </p>
          </label>

          <label className="write__field">
            <span>내용</span>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="만들면서 있었던 일, 팁, 후기를 자유롭게 적어주세요."
              rows={12}
            />
            <ErrorMessage message={errors.content} />
          </label>

          <div className="write__actions">
            <button type="submit" className="btn">
              등록
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
