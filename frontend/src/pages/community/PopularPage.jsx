import { useState } from 'react'
import DateRangeFilter from '../../components/common/DateRangeFilter'
import LikedList from '../../components/common/LikedList'
import { SAMPLE_POPULAR } from '../../data/sampleBoard'
import { shiftDays, toDateKey } from '../../utils/format'

// 인기 레시피 전체 목록 + 기간 검색.
//
// TODO(게시판 담당): 지금은 가데이터(SAMPLE_POPULAR)를 그린다.
//   useEffect 에서 getPopular({ start, end }) 를 부르고, 의존성 배열에 [start, end] 를 넣는다.
export default function PopularPage() {
  // 처음엔 최근 30일
  const [start, setStart] = useState(toDateKey(shiftDays(-29)))
  const [end, setEnd] = useState(toDateKey(new Date()))

  const items = SAMPLE_POPULAR

  const handleSearch = (nextStart, nextEnd) => {
    setStart(nextStart)
    setEnd(nextEnd)
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">인기 레시피</h2>
        <span className="muted section-more">{items.length}건</span>
      </div>

      <p className="muted list-page__desc">선택한 기간 동안 좋아요를 많이 받은 순서입니다.</p>

      <div className="card">
        <DateRangeFilter start={start} end={end} onSearch={handleSearch} />
        <LikedList items={items} />
      </div>
    </div>
  )
}
