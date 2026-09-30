import { useState } from 'react'
import ErrorMessage from '../common/ErrorMessage'
import '../../styles/components/community/NoticeForm.css'

// 공지 쓰기 / 고치기 폼 (관리자만 보인다).
// notice 를 넘기면 고치기, 안 넘기면 새로 쓰기다.
//
// TODO(게시판 담당): handleSubmit 에서 createNotice / updateNotice 를 부른다.
export default function NoticeForm({ notice, onDone, onCancel }) {
  const editing = notice ? true : false

  // 고치기면 원래 값으로 시작한다
  const [title, setTitle] = useState(editing ? notice.title : '')
  const [content, setContent] = useState(editing ? notice.content : '')
  const [pinned, setPinned] = useState(editing ? notice.pinned : false)

  const [titleError, setTitleError] = useState('')
  const [contentError, setContentError] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    setTitleError(title.trim() ? '' : '제목을 입력해주세요')
    setContentError(content.trim() ? '' : '내용을 입력해주세요')
    if (!title.trim() || !content.trim()) {
      return
    }

    const form = {
      title: title.trim(),
      content: content.trim(),
      pinned: pinned,
    }

    // TODO: editing 이면 updateNotice(notice.id, form), 아니면 createNotice(form)
    onDone(form, editing)
  }

  return (
    <form className="notice-form" onSubmit={handleSubmit}>
      <h3 className="notice-form__title">{editing ? '공지 수정' : '새 공지 작성'}</h3>

      <label className="notice-form__field">
        <span>제목</span>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="공지 제목을 입력해주세요"
          maxLength={200}
        />
        <ErrorMessage message={titleError} />
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
          rows={8}
        />
        <ErrorMessage message={contentError} />
      </label>

      <div className="notice-form__actions">
        <button type="button" className="btn btn--ghost btn--sm" onClick={onCancel}>
          취소
        </button>
        <button type="submit" className="btn btn--sm">
          {editing ? '수정' : '등록'}
        </button>
      </div>
    </form>
  )
}
