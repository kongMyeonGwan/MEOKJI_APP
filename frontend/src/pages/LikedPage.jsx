import { useMemo, useState } from 'react'
import DateRangeFilter from '../components/common/DateRangeFilter'
import LikedList from '../components/recommendation/LikedList'
import { MOCK_LIKED_RECIPES } from '../mocks'
import { shiftDays, toDateKey } from '../utils/format'
import '../styles/pages/ListPage.css'

/**
 * 좋아요 받은 레시피 전체 목록 + 기간 검색.
 * 대시보드의 같은 섹션을 넓게 펼친 화면이다.
 */
export default function LikedPage() {
  const [range, setRange] = useState(() => ({
    start: toDateKey(shiftDays(-29)),
    end: toDateKey(new Date()),
  }))

  const items = useMemo(
    () =>
      MOCK_LIKED_RECIPES.filter((item) => {
        const key = toDateKey(item.liked_at)
        return key >= range.start && key <= range.end
      }),
    [range]
  )

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">좋아요 받은 레시피</h2>
        <span className="muted section-more">{items.length}건</span>
      </div>

      <div className="card">
        <DateRangeFilter
          start={range.start}
          end={range.end}
          onSearch={(start, end) => setRange({ start, end })}
        />
        <LikedList items={items} />
      </div>
    </div>
  )
}
