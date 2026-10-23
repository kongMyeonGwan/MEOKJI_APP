import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createNotice, updateNotice } from '../../../../api/notices' // ★ noticeApi → notices

const TITLE_MAX = 200
const CONTENT_MAX = 600 // ★ 200 → 600. 서버 NoticeRequest 의 @Size(max = 600) 와 같은 값. 컬럼이 600 CHAR 여야 한글 600자가 들어간다

// 공지 쓰기 / 고치기 폼 (관리자만 보인다).
// notice 를 넘기면 고치기, 안 넘기면 새로 쓰기다.
// ★ 저장이 끝나면 onSaved(저장된 공지) 를 부르고, onSaved 가 없으면 그 공지의 상세 화면으로 간다.
// ★ 상세 화면의 수정 폼은 같은 주소에 머물러야 해서 onSaved 로 받아 화면 값을 갈아끼운다
// ★ (navigate 로 같은 주소에 가면 화면이 다시 받아오지 않아 옛 내용이 남는다).
export default function NoticeForm({ notice, cancelTo, onCancel, onSaved }) { // ★ onSaved 추가
  const navigate = useNavigate()
  const editing = notice ? true : false

  const [title, setTitle] = useState(editing ? notice.title : '')
  const [content, setContent] = useState(editing ? notice.content : '')
  const [pinned, setPinned] = useState(editing ? notice.pinned : false)

  const [errors, setErrors] = useState({}) // { title, content } 칸 아래 문구
  const [submitError, setSubmitError] = useState('') // 서버가 거절했을 때 문구
  const [saving, setSaving] = useState(false)

  // 비었는지 검사한다. 통과하면 true
  const validate = () => {
    const next = {}
    if (!title.trim()) {
      next.title = '제목을 입력해주세요.'
    }
    if (!content.trim()) {
      next.content = '내용을 입력해주세요.'
    }
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = async () => {
    if (saving || !validate()) {
      return
    }

    setSaving(true)
    setSubmitError('')
    try {
      const form = { title: title.trim(), content, pinned }
      const saved = editing ? await updateNotice(notice.id, form) : await createNotice(form)
      if (onSaved) {                       // ★
        onSaved(saved)                     // ★
      } else {                             // ★
        navigate(`/notice/${saved.id}`)    // ★
      }                                    // ★
    } catch (e) {
      const status = e.response?.status
      if (status === 401) {
        setSubmitError('로그인이 필요합니다.')
      } else if (status === 403) {
        setSubmitError('관리자만 사용할 수 있습니다.')
      } else if (status === 404) {
        setSubmitError('이미 삭제된 공지입니다.')
      } else if (status === 400) {
        setSubmitError('제목과 내용을 확인해주세요.')
      } else {
        setSubmitError('저장하지 못했습니다. 잠시 후 다시 시도해주세요.')
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="notice-form">
      <h3 className="notice-form__title">{editing ? '공지 수정' : '새 공지 작성'}</h3>

      <label className="notice-form__field">
        <span>제목</span>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="공지 제목을 입력해주세요"
          maxLength={TITLE_MAX}
        />
        {errors.title && <p className="muted">{errors.title}</p>}
      </label>

      <label className="notice-form__check">
        <input type="checkbox" checked={pinned} onChange={(e) => setPinned(e.target.checked)} />
        <span>목록 맨 위에 고정</span>
      </label>

      <label className="notice-form__field">
        <span>내용</span>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="공지 내용을 입력해주세요"
          rows={10}
          maxLength={CONTENT_MAX}
        />
        <span className="muted">
          {content.length} / {CONTENT_MAX}
        </span>
        {errors.content && <p className="muted">{errors.content}</p>}
      </label>

      {submitError && <p className="muted">{submitError}</p>}

      <div className="notice-form__actions">
        {/* 취소는 두 가지다. 작성 화면이면 목록으로 나가고(cancelTo),
            상세 화면의 수정 폼이면 폼만 닫는다(onCancel) */}
        {cancelTo ? (
          <Link to={cancelTo} className="btn btn--ghost btn--sm">
            취소
          </Link>
        ) : (
          <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
            취소
          </button>
        )}

        <button type="button" className="btn btn--sm" onClick={handleSubmit} disabled={saving}>
          {saving ? '저장 중...' : editing ? '수정' : '등록'}
        </button>
      </div>
    </div>
  )
}