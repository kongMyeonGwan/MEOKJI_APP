import { useEffect, useState } from 'react'
import DateRangeFilter from '../components/common/DateRangeFilter'
import ErrorMessage from '../components/common/ErrorMessage'
import LikedList from '../components/recommendation/LikedList'
import Loading from '../components/common/Loading'
import { getPopular } from '../api/posts'
import { parseApiError } from '../utils/apiError'
import { shiftDays, toDateKey } from '../utils/format'
import '../styles/pages/ListPage.css'

/**
 * 인기 레시피 전체 목록 + 기간 검색.
 * 대시보드 오른쪽의 순위(PopularRecipes)를 넓게 펼친 화면이다.
 *
 * 집계는 서버가 한다. 고른 기간에 **새로 눌린** 좋아요만 세기 때문에,
 * 목록에 실려 오는 like_count 는 "그 기간의 좋아요 수"이지 누적이 아니다.
 */

/** 한 번에 받아올 개수. 전체보기 화면이라 사이드(8건)보다 넉넉히 잡는다 */
const PAGE_SIZE = 50

/** 처음 열었을 때의 기간: 최근 30일 (오늘 포함이라 -29일부터) */
function getDefaultRange() {
  return {
    start: toDateKey(shiftDays(-29)),
    end: toDateKey(new Date()),
  }
}

export default function PopularPage() {
  // useState 에 함수를 넘기면 첫 렌더에서 한 번만 실행된다
  const [range, setRange] = useState(getDefaultRange)

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  // 기간이 바뀔 때마다 다시 물어본다.
  // alive 플래그는 늦게 도착한 옛 응답이 새 결과를 덮지 않게 막는다.
  useEffect(() => {
    let alive = true
    setLoading(true)
    setLoadError('')

    getPopular({ start: range.start, end: range.end, limit: PAGE_SIZE })
      .then((data) => {
        if (alive) setItems(data)
      })
      .catch((err) => {
        if (alive) setLoadError(parseApiError(err).message)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [range.start, range.end])

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">인기 레시피</h2>
        {!loading && <span className="muted section-more">{items.length}건</span>}
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

        {loadError ? (
          <ErrorMessage message={loadError} />
        ) : loading ? (
          <Loading message="인기 레시피를 불러오는 중..." />
        ) : (
          <LikedList items={items} />
        )}
      </div>
    </div>
  )
}
