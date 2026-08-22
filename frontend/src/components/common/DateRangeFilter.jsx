import { useEffect, useState } from 'react'
import '../../styles/components/common/DateRangeFilter.css'

/**
 * 기간 선택 필터.
 *
 * 값은 'YYYY-MM-DD' 문자열로 주고받는다 (<input type="date">의 형식).
 *
 * 날짜를 고를 때마다 조회하지 않고 '검색'을 눌러야 반영된다.
 * 시작일을 바꾸는 순간에는 아직 종료일을 안 고른 상태라
 * 중간값으로 한 번 더 조회하게 되기 때문이다. 서버 호출이면 낭비다.
 */
export default function DateRangeFilter({ start, end, onSearch }) {
  const [draft, setDraft] = useState({ start, end })

  // 부모가 값을 바꾸면(예: 다른 화면에서 넘어온 경우) 입력칸도 따라간다
  useEffect(() => {
    setDraft({ start, end })
  }, [start, end])

  const onSubmit = (e) => {
    e.preventDefault()
    onSearch(draft.start, draft.end)
  }

  return (
    <form className="range" onSubmit={onSubmit}>
      <input
        type="date"
        className="range__input"
        value={draft.start}
        // 시작일이 종료일보다 뒤로 갈 수 없게 브라우저가 막아준다
        max={draft.end}
        onChange={(e) => setDraft((prev) => ({ ...prev, start: e.target.value }))}
        aria-label="시작일"
      />

      <span className="range__tilde" aria-hidden="true">~</span>

      <input
        type="date"
        className="range__input"
        value={draft.end}
        min={draft.start}
        onChange={(e) => setDraft((prev) => ({ ...prev, end: e.target.value }))}
        aria-label="종료일"
      />

      <button type="submit" className="btn btn--ghost btn--sm range__submit">
        검색
      </button>
    </form>
  )
}
