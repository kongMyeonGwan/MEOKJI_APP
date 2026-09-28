import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import Loading from '../../components/common/Loading'
import { getHistories } from '../../api/histories'
import { createReview, uploadReviewFile } from '../../api/reviews'
import { parseApiError } from '../../utils/apiError'
import '../../styles/pages/ReviewWritePage.css'

// 게시글 쓰기 화면.
//
// 글은 반드시 확정한 식사 기록에서 출발한다. 기록 없이 글만 쓰는 길은 없다.
// DB 에서도 meal_history_id 가 NOT NULL 이라 서버가 한 번 더 막는다.
//
// 화면은 다 붙여뒀다. 다만 서버의 POST /api/reviews 가 아직 501 이라
// api/reviews.js 의 createReview 가 그 전에 막는다 - 등록을 누르면
// "게시판 기능은 아직 준비 중입니다." 가 뜬다.
//
// 게시판 담당자가 서버와 api/reviews.js 를 채우면 이 화면은 그대로 돈다.
//
// 등록이 두 번에 나뉘는 이유
// --------------------------
// 한 요청에 JSON 이랑 파일을 같이 담을 수 없어서 글(JSON)과 사진(multipart)을
// 따로 보낸다. 그래서 사진만 실패할 수 있는데, 그때 되돌리지 않는다 -
// 남는 것은 '사진 없는 멀쩡한 글' 이고 사진은 원래 선택 사항이다.

// 사진 한 장만 붙는다. 서버 설정이랑 같은 값이다.
// 서버가 어차피 다시 검사한다. 여기서 한 번 더 보는 건
// 5MB 짜리를 다 올려보낸 뒤에 거절당하는 일을 없애려는 것이다.
const ALLOWED_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'webp']
const MAX_MB = 5

export default function ReviewWritePage() {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [historyId, setHistoryId] = useState('')
  const [file, setFile] = useState(null)

  const [errors, setErrors] = useState({})

  // 등록 중인지. 두 번 눌러 글이 두 개 올라가는 것을 막는다.
  // DB 의 uq_reviews_meal 이 결국 막아주지만 그때는 409 를 보게 된다.
  const [saving, setSaving] = useState(false)

  const navigate = useNavigate()

  const [histories, setHistories] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    let alive = true

    // 아직 공유 안 한 기록만 서버가 걸러준다.
    //
    // size 는 50 으로 받는다. 드롭다운이라 한 번에 다 보여줘야 하는데
    // 수백 개짜리 드롭다운도 쓸 만하지 않다. 보통 방금 먹은 걸 공유한다.
    getHistories({ shared: false, size: 50 })
      .then(function (data) {
        if (alive) {
          setHistories(data.items)
        }
      })
      .catch(function (err) {
        if (alive) {
          setLoadError(parseApiError(err).message)
        }
      })
      .finally(function () {
        if (alive) {
          setLoading(false)
        }
      })

    return function () {
      alive = false
    }
  }, [])

  // 에러 하나만 바꾼다
  function setFileError(text) {
    const next = Object.assign({}, errors)
    next.file = text
    setErrors(next)
  }

  // 사진을 골랐을 때. 확장자랑 크기를 여기서 한 번 본다.
  function onPickFile(e) {
    let picked = null
    if (e.target.files && e.target.files.length > 0) {
      picked = e.target.files[0]
    }

    if (!picked) {
      setFile(null)
      setFileError(undefined)
      return
    }

    const parts = picked.name.split('.')
    const ext = parts[parts.length - 1].toLowerCase()

    if (!ALLOWED_EXTS.includes(ext)) {
      setFile(null)
      e.target.value = '' // 안 받을 파일을 입력칸에 남겨두지 않는다
      setFileError(ALLOWED_EXTS.join(', ') + ' 파일만 올릴 수 있어요')
      return
    }

    if (picked.size > MAX_MB * 1024 * 1024) {
      setFile(null)
      e.target.value = ''
      setFileError(MAX_MB + 'MB 이하만 올릴 수 있어요')
      return
    }

    setFile(picked)
    setFileError(undefined)
  }

  // 등록 버튼을 눌렀을 때
  async function onSubmit(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    if (saving) {
      return
    }

    const next = {}
    if (!title.trim()) {
      next.title = '제목을 입력해주세요'
    }
    if (!historyId) {
      next.historyId = '공유할 내 기록을 선택해주세요'
    }
    if (!content.trim()) {
      next.content = '내용을 입력해주세요'
    }

    setErrors(next)

    if (Object.keys(next).length > 0) {
      return
    }

    setSaving(true)

    let created
    try {
      created = await createReview({
        mealHistoryId: historyId,
        title: title.trim(),
        content: content,
      })
    } catch (err) {
      // 이미 공유한 기록이면 409 다. 다시 눌러도 또 409 라서 재시도시키지
      // 않고, 어느 칸이 문제인지 드롭다운 아래에 붙여준다.
      const parsed = parseApiError(err)
      let message = parsed.message
      if (!message) {
        message = '글을 등록하지 못했습니다.'
      }

      setErrors({ historyId: message })
      setSaving(false)
      return
    }

    // 사진은 따로 보낸다. 실패해도 글은 이미 올라가 있으니 되돌리지 않는다 -
    // 남는 것은 '사진 없는 멀쩡한 글' 이고, 상세 화면에서 다시 붙일 수 있다.
    if (file) {
      try {
        await uploadReviewFile(created.id, file)
      } catch (err) {
        window.alert('글은 올라갔지만 사진을 올리지 못했습니다. 상세 화면에서 다시 붙여주세요.')
      }
    }

    // 목록이 아니라 방금 쓴 글로 보낸다. 등록된 모습을 바로 확인할 수 있어야 한다.
    navigate('/community/' + created.id)
  }

  let fileHelp = '한 장까지 올릴 수 있어요. ' + MAX_MB + 'MB 이하 이미지.'
  if (file) {
    fileHelp = file.name + ' — 등록하면 함께 올라갑니다'
  }

  // <input type="file"> 의 accept 속성에 넣을 문자열
  const acceptText = '.jpg,.jpeg,.png,.gif,.webp'

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

      {!loading && loadError && <ErrorMessage message={loadError} />}

      {!loading && !loadError && histories.length === 0 && (
        <p className="muted write__help">
          공유할 수 있는 기록이 없습니다. 식사를 확정하면 이곳에서 글을 쓸 수
          있어요. <Link to="/history">내 기록 보기</Link>
        </p>
      )}

      {!loading && !loadError && histories.length > 0 && (
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
            <select
              value={historyId}
              onChange={(e) => setHistoryId(e.target.value)}
            >
              <option value="">기록을 선택해주세요</option>
              {histories.map(function (h) {
                return (
                  <option key={h.id} value={h.id}>
                    {h.food_name} — {h.recipe_name}
                  </option>
                )
              })}
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

          <label className="write__field">
            <span>사진 (선택)</span>
            <input type="file" accept={acceptText} onChange={onPickFile} />
            <ErrorMessage message={errors.file} />
            <p className="muted write__help">{fileHelp}</p>
          </label>

          <div className="write__actions">
            <button type="submit" className="btn" disabled={saving}>
              {saving ? '등록 중…' : '등록'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
