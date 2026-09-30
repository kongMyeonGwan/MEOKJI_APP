import { useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import DateRangeFilter from '../common/DateRangeFilter'
import { SAMPLE_POPULAR } from '../../data/sampleBoard'
import { shiftDays, toDateKey } from '../../utils/format'
import '../../styles/components/recommendation/PopularRecipes.css'

// 인기 레시피 순위 (첫 화면 오른쪽). 기간 안에 좋아요를 많이 받은 순.
//
// TODO(게시판 담당): 지금은 가데이터(SAMPLE_POPULAR)를 그린다.
//   useEffect 에서 getPopular({ start, end, limit: 8 }) 를 부르고,
//   의존성 배열에 [start, end] 를 넣어서 기간이 바뀔 때마다 다시 부른다.

const PRESETS = [
  { key: 'TODAY', label: '오늘', days: 0 },
  { key: 'D7', label: '7일', days: 6 },
  { key: 'D30', label: '30일', days: 29 },
]

export default function PopularRecipes() {
  // 지금 켜진 탭: 'TODAY' / 'D7' / 'D30' / 'CUSTOM'(직접 선택)
  const [preset, setPreset] = useState('D30')
  const [start, setStart] = useState(toDateKey(shiftDays(-29)))
  const [end, setEnd] = useState(toDateKey(new Date()))

  const items = SAMPLE_POPULAR

  // 오늘 / 7일 / 30일 탭을 눌렀을 때
  const choosePreset = (item) => {
    setPreset(item.key)
    setStart(toDateKey(shiftDays(-item.days)))
    setEnd(toDateKey(new Date()))
  }

  // 날짜를 직접 골랐을 때
  const applyCustom = (nextStart, nextEnd) => {
    setPreset('CUSTOM')
    setStart(nextStart)
    setEnd(nextEnd)
  }

  const current = PRESETS.find((item) => item.key === preset)
  const rangeTitle = current ? current.label : '직접 선택한 기간'

  return (
    <section className="card popular">
      <div className="section-head">
        <h2 className="section-title">
          🔥 인기 레시피 <span className="popular__range">({rangeTitle})</span>
        </h2>
        <Link to="/popular" className="section-more">
          더보기 ›
        </Link>
      </div>

      <div className="chip-row popular__tabs">
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
        <button
          type="button"
          className={preset === 'CUSTOM' ? 'chip is-active' : 'chip'}
          onClick={() => setPreset('CUSTOM')}
        >
          기간 선택 📅
        </button>
      </div>

      <DateRangeFilter start={start} end={end} onSearch={applyCustom} />

      <p className="popular__note muted">* 선택한 기간 동안 새로 받은 좋아요 수 기준</p>

      {items.length === 0 ? (
        <p className="empty">이 기간에 좋아요를 받은 레시피가 없습니다.</p>
      ) : (
        <ol className="popular__list">
          {items.map((item, index) => (
            <li key={item.id}>
              <Link to={`/recipe/${item.food_name}`} className="popular__row">
                <span className="popular__rank">{index + 1}</span>
                <FoodThumb item={item} size="md" />
                <span className="popular__body">
                  <span className="popular__name">{item.recipe_name || item.food_name}</span>
                  <span className="popular__likes">♥ {item.like_count}</span>
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}

      <Link to="/popular" className="btn btn--outline btn--block popular__all">
        인기 레시피 전체보기 →
      </Link>
    </section>
  )
}
