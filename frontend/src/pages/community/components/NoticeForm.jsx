import { useState } from 'react'
import ErrorMessage from '../../../components/common/ErrorMessage'
import useForm from '../../../hooks/useForm'

// 공지 쓰기 / 고치기 폼 (관리자만 보인다).
// notice 를 넘기면 고치기, 안 넘기면 새로 쓰기다.
//
// TODO(게시판 담당): handleSubmit 에서 createNotice / updateNotice 를 부른다.

// 입력값을 검사해서 칸별 에러 문구를 돌려준다. 에러가 없으면 빈 객체 {}
const validate = (v) => {
  const errors = {}
  if (!v.title.trim()) {
    errors.title = '제목을 입력해주세요'
  }
  if (!v.content.trim()) {
    errors.content = '내용을 입력해주세요'
  }
  return errors
}

export default function NoticeForm({ notice, onDone, onCancel }) {
  const editing = notice ? true : false

  // 고치기면 원래 값으로 시작한다
  const { values, handleChange } = useForm({
    title: editing ? notice.title : '',
    content: editing ? notice.content : '',
    pinned: editing ? notice.pinned : false, // 체크박스도 handleChange 하나로 된다
  })

  const [errors, setErrors] = useState({}) // 예) { title: '제목을 입력해주세요' }

  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    const nextErrors = validate(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }

    const form = {
      title: values.title.trim(),
      content: values.content.trim(),
      pinned: values.pinned,
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
          name="title"
          value={values.title}
          onChange={handleChange}
          placeholder="공지 제목을 입력해주세요"
          maxLength={200}
        />
        <ErrorMessage message={errors.title} />
      </label>

      <label className="notice-form__check">
        <input type="checkbox" name="pinned" checked={values.pinned} onChange={handleChange} />
        <span>목록 맨 위에 고정</span>
      </label>

      <label className="notice-form__field">
        <span>내용</span>
        <textarea
          name="content"
          value={values.content}
          onChange={handleChange}
          placeholder="공지 내용을 입력해주세요"
          rows={8}
        />
        <ErrorMessage message={errors.content} />
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
