import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import Loading from '../../components/common/Loading'
import { getHistories } from '../../api/histories'
import { createReview, uploadReviewFile } from '../../api/reviews'
import '../../styles/pages/ReviewWritePage.css'

// 글쓰기 화면 (/community/write).
// 글은 내 식사 기록(History) 하나를 골라서 쓴다. 사진은 선택.
//
// TODO(게시판 담당): 서버의 글쓰기 API 가 아직 없어서 등록을 누르면 "준비 중" 에러가 뜬다.
//   api/reviews.js 의 createReview, uploadReviewFile 을 채우면 이 화면은 그대로 동작한다.
export default function ReviewWritePage() {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [historyId, setHistoryId] = useState('') // 고른 식사 기록 id
  const [file, setFile] = useState(null) // 고른 사진 파일

  const [histories, setHistories] = useState([]) // 드롭다운에 보여줄 내 기록들
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const navigate = useNavigate()

  // 아직 공유 안 한 내 식사 기록을 받아온다
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getHistories({ shared: false, size: 50 })
        setHistories(data.items)
      } catch (err) {
        // 서버가 보낸 오류 문장을 그대로 보여준다.
        if (err.response) {
          // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
          const detail = err.response.data.detail
          if (Array.isArray(detail)) {
            setError(detail[0].msg)
          } else {
            setError(detail || '요청을 처리하지 못했습니다.')
          }
        } else if (err.request) {
          // 백엔드를 안 켰거나 30초가 지난 경우
          setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
        } else {
          // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
          setError(err.message)
        }
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  // 사진을 골랐을 때. 5MB 가 넘으면 안 받는다.
  const handleFileChange = (e) => {
    const picked = e.target.files[0]

    if (picked && picked.size > 5 * 1024 * 1024) {
      alert('5MB 이하 사진만 올릴 수 있어요')
      e.target.value = '' // 입력칸 비우기
      setFile(null)
      return
    }

    setFile(picked || null)
  }

  // 등록 버튼
  const handleSubmit = async (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    if (!title.trim()) {
      setError('제목을 입력해주세요')
      return
    }
    if (!historyId) {
      setError('공유할 내 기록을 선택해주세요')
      return
    }
    if (!content.trim()) {
      setError('내용을 입력해주세요')
      return
    }

    setError('')
    setSaving(true)
    try {
      // 1. 글 등록
      const created = await createReview({
        mealHistoryId: historyId,
        title: title.trim(),
        content: content,
      })

      // 2. 사진이 있으면 따로 올린다
      if (file) {
        await uploadReviewFile(created.id, file)
      }

      // 방금 쓴 글로 이동
      navigate(`/community/${created.id}`)
    } catch (err) {
      // 서버가 보낸 오류 문장을 그대로 보여준다.
      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          setError(detail[0].msg)
        } else {
          setError(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
        setError(err.message)
      }
    } finally {
      setSaving(false)
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

      {loading && <Loading message="내 기록을 불러오는 중..." />}

      {!loading && histories.length === 0 && (
        <p className="muted write__help">
          공유할 수 있는 기록이 없습니다. 레시피에서 "이걸로 먹을래요" 를 누르면 여기서 글을 쓸 수 있어요.{' '}
          <Link to="/history">내 기록 보기</Link>
        </p>
      )}

      {!loading && histories.length > 0 && (
        <form onSubmit={handleSubmit}>
          <label className="write__field">
            <span>제목</span>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="어떤 이야기인가요?"
              maxLength={200}
            />
          </label>

          <label className="write__field">
            <span>공유할 내 기록</span>
            <select value={historyId} onChange={(e) => setHistoryId(e.target.value)}>
              <option value="">기록을 선택해주세요</option>
              {histories.map((history) => (
                <option key={history.id} value={history.id}>
                  {history.food_name} — {history.recipe_name}
                </option>
              ))}
            </select>
            <p className="muted write__help">선택한 기록의 레시피와 재료가 게시글에 함께 표시됩니다.</p>
          </label>

          <label className="write__field">
            <span>내용</span>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="만들면서 있었던 일, 팁, 후기를 자유롭게 적어주세요."
              rows={12}
            />
          </label>

          <label className="write__field">
            <span>사진 (선택)</span>
            <input type="file" accept=".jpg,.jpeg,.png,.gif,.webp" onChange={handleFileChange} />
            <p className="muted write__help">
              {file ? `${file.name} — 등록하면 함께 올라갑니다` : '한 장까지 올릴 수 있어요. 5MB 이하 이미지.'}
            </p>
          </label>

          <ErrorMessage message={error} />

          <div className="write__actions">
            <button type="submit" className="btn" disabled={saving}>
              {saving ? '등록 중…' : '등록'}
            </button>
          </div>
        </form>
      )}

      {/* 기록을 못 불러왔을 때 */}
      {!loading && histories.length === 0 && <ErrorMessage message={error} />}
    </div>
  )
}
