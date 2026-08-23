import { useMemo, useState } from 'react'
import DateRangeFilter from '../components/common/DateRangeFilter'
import LikedList from '../components/recommendation/LikedList'
import { MOCK_LIKED_RECIPES } from '../mocks'
import { shiftDays, toDateKey } from '../utils/format'
import '../styles/pages/ListPage.css'

/**
 * 인기 레시피 전체 목록 + 기간 검색.
 * 대시보드 오른쪽의 순위(PopularRecipes)를 넓게 펼친 화면이다.
 */
export default function PopularPage() {
  const [range, setRange] = useState(() => ({
    start: toDateKey(shiftDays(-29)),
    end: toDateKey(new Date()),
  }))

  // 대시보드와 같은 정렬을 쓴다. 여기는 상위 8건으로 자르지 않을 뿐이다.
  // 정렬이 다르면 "전체보기"를 눌렀을 때 순서가 뒤바뀌어 보인다.
  const items = useMemo(
    () =>
      MOCK_LIKED_RECIPES.filter((item) => {
        const key = toDateKey(item.liked_at)
        return key >= range.start && key <= range.end
      }).sort(
        (a, b) =>
          b.like_count - a.like_count ||
          new Date(b.liked_at) - new Date(a.liked_at)
      ),
    [range]
  )

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
