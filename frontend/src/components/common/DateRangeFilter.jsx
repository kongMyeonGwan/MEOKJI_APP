import { useEffect, useState } from 'react'

// 기간 고르는 필터. 날짜는 'YYYY-MM-DD' 글자로 주고받는다.
// 날짜를 바꾸고 '검색' 을 눌러야 부모에게 알려준다.
//
// 왜 바로 알리지 않나 — 시작일을 고치는 동안 종료일은 아직 옛 값이다.
// 그때마다 서버를 부르면 "2026-01-01 ~ 2026-09-30" 처럼 엉뚱한
// 기간으로 한 번 조회된다. 요청도 그만큼 늘어난다.
//
// Date 객체가 아니라 글자로 주고받는 이유 — <input type="date"> 가
// 글자만 받는다. 그리고 서버도 "2026-09-01" 모양을 받는다 -
// 중간에 Date 로 바꿀 이유가 없다.
//
// 쓰는 곳 — 인기 레시피 화면 (기간 안에 좋아요를 많이 받은 순)
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
    // 안 막으면 브라우저가 폼을 서버로 보내면서 화면이 통째로
    // 새로고침된다. 리액트가 들고 있던 상태가 전부 사라진다
    onSearch(draftStart, draftEnd)
  }

  // div 가 아니라 form 으로 감싼다. 그러면 날짜 칸에서 엔터를 쳐도
  // 검색이 된다 - 버튼을 눌러야만 되게 두면 엔터가 안 먹어 답답하다
  return (
    <form className="range" onSubmit={handleSubmit}>
      <input
        type="date"
        className="range__input"
        value={draftStart}
        // 종료일보다 뒤를 못 고르게 막는다. 날짜 선택 창에서
        // 그 뒤가 회색으로 비활성화된다
        max={draftEnd}
        onChange={(e) => setDraftStart(e.target.value)}
      />
      <span className="range__tilde">~</span>
      <input
        type="date"
        className="range__input"
        value={draftEnd}
        // 시작일보다 앞을 못 고르게 막는다. 둘이 서로를 막아주므로
        // 뒤집힌 기간이 만들어지지 않는다
        min={draftStart}
        onChange={(e) => setDraftEnd(e.target.value)}
      />
      {/* type="submit" 이라 위 onSubmit 이 불린다.
          onClick 을 달지 않은 이유 — 엔터로도 같은 일이 되게
          하려면 onSubmit 한 곳에 모으는 쪽이 맞다 */}
      <button type="submit" className="btn btn--ghost btn--sm range__submit">
        검색
      </button>
    </form>
  )
}
