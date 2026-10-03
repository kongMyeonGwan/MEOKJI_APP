import { useState } from 'react'

// 기간 고르는 필터. 날짜는 'YYYY-MM-DD' 글자로 주고받는다.
// 날짜를 바꾸고 '검색' 을 눌러야 부모에게 알려준다.
export default function DateRangeFilter({ start, end, onSearch }) {
  const [draftStart, setDraftStart] = useState(start)
  const [draftEnd, setDraftEnd] = useState(end)

  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    onSearch(draftStart, draftEnd)
  }

  return (
    <form className="range" onSubmit={handleSubmit}>
      <input
        type="date"
        className="range__input"
        value={draftStart}
        max={draftEnd}
        onChange={(e) => setDraftStart(e.target.value)}
      />
      <span className="range__tilde">~</span>
      <input
        type="date"
        className="range__input"
        value={draftEnd}
        min={draftStart}
        onChange={(e) => setDraftEnd(e.target.value)}
      />
      <button type="submit" className="btn btn--ghost btn--sm range__submit">
        검색
      </button>
    </form>
  )
}
