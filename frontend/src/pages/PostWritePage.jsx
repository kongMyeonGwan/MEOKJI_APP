import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import { MOCK_HISTORIES } from '../mocks'
import '../styles/pages/PostWritePage.css'

/**
 * 게시글 작성 (STEP 20).
 *
 * History에서 고른 음식을 함께 첨부할 수 있다.
 * 첨부하면 food_name과 recipe_json이 게시글에 복사된다.
 */
export default function PostWritePage() {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [historyId, setHistoryId] = useState('')
  const [errors, setErrors] = useState({})

  const navigate = useNavigate()

  const onSubmit = (e) => {
    e.preventDefault()

    const next = {}
    if (!title.trim()) next.title = '제목을 입력해주세요'
    if (!content.trim()) next.content = '내용을 입력해주세요'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    // 서버 연동 전이라 목록으로만 돌려보낸다.
    navigate('/community')
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
          <span>
            내 기록 첨부 <span className="muted">(선택)</span>
          </span>
          <select value={historyId} onChange={(e) => setHistoryId(e.target.value)}>
            <option value="">첨부하지 않음</option>
            {MOCK_HISTORIES.map((h) => (
              <option key={h.id} value={h.id}>
                {h.food_name} — {h.recipe_name}
              </option>
            ))}
          </select>
          <p className="muted write__help">
            첨부하면 레시피가 게시글에 함께 표시됩니다.
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
    </div>
  )
}
