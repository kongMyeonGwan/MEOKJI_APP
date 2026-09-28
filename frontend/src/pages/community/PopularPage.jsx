import { useState } from 'react'
import DateRangeFilter from '../../components/common/DateRangeFilter'
import LikedList from '../../components/recommendation/LikedList'
import { SAMPLE_POPULAR } from '../../data/sampleBoard'
import { shiftDays, toDateKey } from '../../utils/format'
import '../../styles/pages/ListPage.css'

// 인기 레시피 전체 목록 + 기간 검색.
// 첫 화면 오른쪽에 있는 순위를 넓게 펼친 화면이다.
//
// ! 아직 서버에 붙어 있지 않다. 게시판 담당자가 채운다.
//
// 인기 레시피는 게시글 좋아요(review_likes)를 기간으로 집계한 결과라서
// 게시판이 완성돼야 값이 나온다. 지금은 하드코딩 데이터를 그린다.
//
// 붙일 때 할 일
// -------------
//   1. useEffect 로 getPopular({ start, end, limit: PAGE_SIZE }) 를 부른다
//   2. 기간이 바뀔 때마다 다시 부른다 (의존성 [start, end])
//   3. 요청이 보낸 순서대로 도착하지 않으니 alive 플래그로
//      늦게 온 응답이 새 결과를 덮지 않게 막는다

// 한 번에 받아올 개수. 전체보기 화면이라 첫 화면(8건)보다 넉넉히 잡았다.
const PAGE_SIZE = 50

export default function PopularPage() {
  // 처음 열었을 때는 최근 30일 (오늘 포함이라 -29일부터)
  const [start, setStart] = useState(toDateKey(shiftDays(-29)))
  const [end, setEnd] = useState(toDateKey(new Date()))

  // TODO: 서버에서 받아오게 바꾼다. 지금은 하드코딩 데이터다.
  const items = SAMPLE_POPULAR

  // 기간 검색 버튼을 눌렀을 때
  function onSearch(nextStart, nextEnd) {
    setStart(nextStart)
    setEnd(nextEnd)
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">인기 레시피</h2>
        <span className="muted section-more">{items.length}건</span>
      </div>

      <p className="muted list-page__desc">
        선택한 기간 동안 좋아요를 많이 받은 순서입니다.
      </p>

      <div className="card">
        <DateRangeFilter start={start} end={end} onSearch={onSearch} />

        <LikedList items={items} />
      </div>
    </div>
  )
}
