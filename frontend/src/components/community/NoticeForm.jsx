import { useState } from 'react'
import ErrorMessage from '../common/ErrorMessage'
import '../../styles/components/community/NoticeForm.css'

// 공지 쓰기 / 고치기 폼 (관리자만 보인다).
//
// 게시글은 /community/write 라는 페이지가 따로 있는데 공지는 항목이
// 셋뿐(제목, 고정, 본문)이라 목록 위에 그냥 펼친다.
//
// 쓰기랑 고치기를 한 컴포넌트로 만들었다. 입력칸이 똑같고 다른 건
// 처음 값이 비어 있느냐랑 POST 냐 PUT 이냐 둘뿐이라서다.
// notice 를 넘기면 고치기, 안 넘기면 새로 쓰기다.
//
// ! 아직 서버에 붙어 있지 않다. 공지 담당자가 채운다.
//   아래 onSubmit 의 TODO 자리에서 createNotice / updateNotice 를 부르고,
//   저장된 공지를 onDone(saved, editing) 으로 부모에게 넘기면 된다.

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

  function onSubmit(e) {
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

    const payload = {
      title: title.trim(),
      content: content.trim(),
      pinned: pinned,
    }

    // TODO: 고치기면 updateNotice(notice.id, payload),
    //   새로 쓰기면 createNotice(payload) 를 부른다.
    //   그리고 서버가 돌려준 공지를 onDone(saved, editing) 으로 넘긴다.
    //   editing 을 같이 넘기는 이유는 새 공지는 1페이지에 있고
    //   고친 공지는 원래 페이지에 그대로 있어서 부모가 구분해야 하기 때문이다.
    onDone(payload, editing)
  }

  let formTitle = '새 공지 작성'
  let submitLabel = '등록'
  if (editing) {
    formTitle = '공지 수정'
    submitLabel = '수정'
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

      <div className="notice-form__actions">
        <button
          type="button"
          className="btn btn--ghost btn--sm"
          onClick={onCancel}
        >
          취소
        </button>
        <button type="submit" className="btn btn--sm">
          {submitLabel}
        </button>
      </div>
    </form>
  )
}
