import { useState } from 'react'
import ErrorMessage from '../common/ErrorMessage'
import { createNotice, updateNotice } from '../../api/notices'
import { parseApiError } from '../../utils/apiError'
import '../../styles/components/community/NoticeForm.css'

// 공지 쓰기 / 고치기 폼 (관리자만 보인다).
//
// 게시글은 /community/write 라는 페이지가 따로 있는데 공지는 항목이
// 셋뿐(제목, 고정, 본문)이라 목록 위에 그냥 펼친다.
//
// 쓰기랑 고치기를 한 컴포넌트로 만들었다. 입력칸이 똑같고 다른 건
// 처음 값이 비어 있느냐랑 POST 냐 PUT 이냐 둘뿐이라서다.
// notice 를 넘기면 고치기, 안 넘기면 새로 쓰기다.

export default function NoticeForm({ notice, onDone, onCancel }) {
  const editing = Boolean(notice)

  // 고치기면 기존 값으로 시작한다
  let firstTitle = ''
  let firstContent = ''
  let firstPinned = false

  if (notice) {
    firstTitle = notice.title
    firstContent = notice.content
    firstPinned = notice.pinned
  }

  const [title, setTitle] = useState(firstTitle)
  const [content, setContent] = useState(firstContent)
  const [pinned, setPinned] = useState(firstPinned)

  const [errors, setErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    // 서버도 똑같이 검사한다. 여기서 한 번 더 보는 건 서버까지 갔다 오지 않고
    // 바로 알려주려는 것이다.
    const next = {}
    if (!title.trim()) {
      next.title = '제목을 입력해주세요'
    }
    if (!content.trim()) {
      next.content = '내용을 입력해주세요'
    }

    setErrors(next)

    if (Object.keys(next).length > 0) {
      return
    }

    setSubmitError('')
    setSubmitting(true)

    const payload = {
      title: title.trim(),
      content: content.trim(),
      pinned,
    }

    try {
      let saved = null

      if (editing) {
        saved = await updateNotice(notice.id, payload)
      } else {
        saved = await createNotice(payload)
      }

      // 저장한 공지를 부모에게 넘긴다. 목록은 부모가 다시 받아온다.
      // 고정 여부가 바뀌면 순서가 달라지는데 그 정렬은 서버가 정하는 거라
      // 화면에서 배열에 직접 끼워 넣으면 서버랑 순서가 달라진다.
      //
      // editing 도 같이 넘긴다. 새로 쓴 공지는 1페이지에 있고 고친 공지는
      // 원래 있던 페이지에 그대로 있어서 부모가 구분해야 한다.
      onDone(saved, editing)
    } catch (err) {
      const parsed = parseApiError(err)

      // 서버가 준 422 는 칸별로 나눠서 각 입력칸 아래에 붙인다
      setErrors({
        title: parsed.fieldErrors.title,
        content: parsed.fieldErrors.content,
      })
      setSubmitError(parsed.message)
      setSubmitting(false)
    }
  }

  let formTitle = '새 공지 작성'
  let submitLabel = '등록'
  if (editing) {
    formTitle = '공지 수정'
    submitLabel = '수정'
  }
  if (submitting) {
    submitLabel = '저장하는 중...'
  }

  return (
    <form className="notice-form" onSubmit={onSubmit} noValidate>
      <h3 className="notice-form__title">{formTitle}</h3>

      <label className="notice-form__field">
        <span>제목</span>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="공지 제목을 입력해주세요"
          maxLength={200}
          autoFocus
        />
        <ErrorMessage message={errors.title} />
      </label>

      {/* 고정은 체크박스 하나뿐이라 라벨을 따로 안 뒀다 */}
      <label className="notice-form__check">
        <input
          type="checkbox"
          checked={pinned}
          onChange={(e) => setPinned(e.target.checked)}
        />
        <span>목록 맨 위에 고정</span>
      </label>

      <label className="notice-form__field">
        <span>내용</span>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder={'줄바꿈은 그대로 보입니다.\n필요한 만큼 여러 줄로 적어주세요.'}
          rows={8}
        />
        <ErrorMessage message={errors.content} />
      </label>

      <ErrorMessage message={submitError} />

      <div className="notice-form__actions">
        <button
          type="button"
          className="btn btn--ghost btn--sm"
          onClick={onCancel}
          disabled={submitting}
        >
          취소
        </button>
        <button type="submit" className="btn btn--sm" disabled={submitting}>
          {submitLabel}
        </button>
      </div>
    </form>
  )
}
