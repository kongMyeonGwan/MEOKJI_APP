import { useEffect, useState } from 'react'

// 기간 고르는 필터. 날짜는 'YYYY-MM-DD' 글자로 주고받는다.
// 날짜를 바꾸고 '검색' 을 눌러야 부모에게 알려준다.
export default function DateRangeFilter({ start, end, onSearch }) {
  // 입력칸에 치는 중인 값. 부모의 start · end 를 그대로 쓰지 않는 이유는,
  // 날짜를 고르는 동안에는 아직 검색하면 안 되기 때문이다.
  const [draftStart, setDraftStart] = useState(start)
  const [draftEnd, setDraftEnd] = useState(end)

  // 부모가 기간을 바꿨으면(예: '오늘' 칩) 입력칸도 따라간다.
  //
  // useState 의 초기값은 처음 그릴 때 한 번만 쓰인다. 이 useEffect 가 없으면
  // 칩을 눌러도 부모 값만 바뀌고 화면의 날짜 칸은 그대로라서, 버튼이 안 먹는
  // 것처럼 보인다.
  useEffect(() => {
    setDraftStart(start)
    setDraftEnd(end)
  }, [start, end])

  const handleSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    onSearch(draftStart, draftEnd)
  }

  return (
    <form className="range" onSubmit={handleSubmit}>
      <label className="range__field">
        <span className="range__label">시작일</span>
        <input
          type="date"
          className="range__input"
          value={draftStart}
          max={draftEnd}
          onChange={(e) => setDraftStart(e.target.value)}
        />
      </label>
      <span className="range__separator" aria-hidden="true">→</span>
      <label className="range__field">
        <span className="range__label">종료일</span>
        <input
          type="date"
          className="range__input"
          value={draftEnd}
          min={draftStart}
          onChange={(e) => setDraftEnd(e.target.value)}
        />
      </label>
      <button type="submit" className="btn btn--ghost btn--sm range__submit">
        검색
      </button>
    </form>
  )
}
