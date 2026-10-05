import { useState } from 'react'
import DateRangeFilter from '../../../components/common/DateRangeFilter'
import PopularCard from './components/PopularCard'
import { SAMPLE_POPULAR } from '../../../data/sampleBoard'
import { shiftDays, toDateKey } from '../../../utils/format'

// 인기 레시피 전체 목록 + 기간 검색. 좋아요가 많은 순으로 카드를 늘어놓는다.
//
// TODO(게시판 담당)
//   - useEffect 에서 getPopular({ start, end }) 를 부른다 (의존성 배열에 [start, end])
//   - 지금은 기간을 바꿔도 가데이터(SAMPLE_POPULAR)라 목록이 그대로다

// 첫 화면의 인기 레시피(home/components/PopularRecipes)와 같은 방식이다.
// days 는 오늘로부터 며칠 전까지 볼지다.
const PRESETS = [
  { key: 'ALL', label: '전체', days: 364 },
  { key: 'TODAY', label: '오늘', days: 0 },
  { key: 'WEEK', label: '이번 주', days: 6 },
  { key: 'MONTH', label: '이번 달', days: 29 },
]

export default function PopularPage() {
  // 지금 켜진 칩: 'ALL' / 'TODAY' / 'WEEK' / 'MONTH' / 'CUSTOM'(직접 선택)
  const [preset, setPreset] = useState('ALL')
  const [start, setStart] = useState(toDateKey(shiftDays(-364)))
  const [end, setEnd] = useState(toDateKey(new Date()))

  const items = SAMPLE_POPULAR

  // 전체 / 오늘 / 이번 주 / 이번 달 칩을 눌렀을 때
  const choosePreset = (item) => {
    setPreset(item.key)
    setStart(toDateKey(shiftDays(-item.days)))
    setEnd(toDateKey(new Date()))
  }

  // 날짜를 직접 골라 '검색' 을 눌렀을 때
  const applyCustom = (nextStart, nextEnd) => {
    setPreset('CUSTOM')
    setStart(nextStart)
    setEnd(nextEnd)
  }

  return (
    <div className="list-page list-page--wide">
      <header className="popular-head">
        <div>
          <h2 className="popular-head__title">👑 인기 레시피</h2>
          <p className="muted popular-head__desc">선택한 기간 동안 좋아요를 많이 받은 레시피입니다.</p>
        </div>
        <p className="popular-head__hello">오늘도 맛있는 하루 되세요! ♡</p>
      </header>

      <div className="card popular-filter">
        <DateRangeFilter start={start} end={end} onSearch={applyCustom} />

        <div className="chip-row popular-filter__chips">
          {PRESETS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={preset === item.key ? 'chip is-active' : 'chip'}
              onClick={() => choosePreset(item)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {items.length === 0 ? (
        <p className="empty">이 기간에 좋아요를 받은 레시피가 없습니다.</p>
      ) : (
        <div className="popular-grid">
          {/* index 가 0 부터라 순위는 +1 한다 */}
          {items.map((item, index) => (
            <PopularCard key={item.id} item={item} rank={index + 1} />
          ))}
        </div>
      )}
    </div>
  )
}
