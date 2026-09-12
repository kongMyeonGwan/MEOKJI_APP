import { useState } from 'react'
import ErrorMessage from '../common/ErrorMessage'
import { createNotice, updateNotice } from '../../api/notices'
import { parseApiError } from '../../utils/apiError'
import '../../styles/components/community/NoticeForm.css'

/**
 * 공지 작성 · 수정 폼 (관리자만).
 *
 * 왜 페이지가 아니라 그 자리에 펴는 폼인가
 * ----------------------------------------
 * 게시글은 /community/write 라는 페이지가 따로 있다. 글을 쓰려면 공유할 내
 * 기록을 골라야 하고, 사진도 붙고, 본문이 길어서 화면이 하나 필요하다.
 *
 * 공지는 항목이 셋뿐이다(제목 · 고정 · 본문). 목록 위에 펴면 방금
 * 쓴 공지가 어디에 끼는지 바로 보이고, 취소해도 돌아올 곳이 없다.
 * NoticeList 가 상세 페이지 없이 그 자리에서 펼치는 것과 같은 이유다.
 *
 * 작성과 수정을 한 컴포넌트로 둔 이유
 * -----------------------------------
 * 두 화면의 입력칸이 똑같다. 다른 건 "처음 값이 비어 있나 채워져
 * 있나"와 "POST 냐 PUT 이냐" 둘뿐이라, 나누면 같은 폼을 두 벌 관리하게 된다.
 * notice 를 주면 수정, 안 주면 작성이다.
 */

export default function NoticeForm({ notice, onDone, onCancel }) {
  const editing = Boolean(notice)

  // 수정이면 기존 값으로 시작한다. useState 의 초기값은 첫 렌더에서만 쓰이므로,
  // 폼을 닫았다 다시 열면(부모가 key 로 새로 만든다) 그때 값이 다시 잡힌다.
  const [title, setTitle] = useState(notice?.title ?? '')
  const [content, setContent] = useState(notice?.content ?? '')
  const [pinned, setPinned] = useState(notice?.pinned ?? false)

  const [errors, setErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e) {
    e.preventDefault() // 폼 기본 동작(새로고침)을 막는다

    // 서버도 똑같이 검사한다(require_text). 여기서 한 번 더 보는 건 막으려는
    // 게 아니라, 왕복 한 번 없이 바로 알려주려는 것이다.
    const next = {}
    if (!title.trim()) next.title = '제목을 입력해주세요'
    if (!content.trim()) next.content = '내용을 입력해주세요'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSubmitError('')
    setSubmitting(true)

    const payload = {
      title: title.trim(),
      content: content.trim(),
      pinned,
    }

    try {
      const saved = editing
        ? await updateNotice(notice.id, payload)
        : await createNotice(payload)

      // 저장된 공지를 부모에게 넘긴다. 부모가 목록을 다시 받아온다 —
      // 고정 여부가 바뀌면 순서가 달라지는데, 그 정렬은 서버 규칙이라
      // 화면이 배열을 직접 끼워 넣으면 서버와 다른 순서가 된다.
      //
      // editing 을 같이 넘기는 이유: 새로 쓴 공지는 최신이라 1페이지에 있고,
      // 고친 공지는 원래 있던 페이지에 그대로 있다. 부모가 어디로 갈지
      // 정하려면 둘을 구분해야 한다.
      onDone(saved, editing)
    } catch (err) {
      const { fieldErrors, message } = parseApiError(err)

      // 서버의 422 는 칸별로 쪼개 각 입력칸 아래에 붙인다
      setErrors({
        title: fieldErrors.title,
        content: fieldErrors.content,
      })
      setSubmitError(message)
      setSubmitting(false)
    }
  }

  return (
    <form className="notice-form" onSubmit={onSubmit} noValidate>
      <h3 className="notice-form__title">
        {editing ? '공지 수정' : '새 공지 작성'}
      </h3>

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

      {/* 고정은 체크박스다. 항목이 하나뿐이라 라벨을 따로 두지 않는다 */}
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
          {submitting ? '저장하는 중...' : editing ? '수정' : '등록'}
        </button>
      </div>
    </form>
  )
}
