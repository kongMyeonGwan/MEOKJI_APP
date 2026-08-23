import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import DateRangeFilter from '../common/DateRangeFilter'
import { MOCK_LIKED_RECIPES } from '../../mocks'
import { shiftDays, toDateKey } from '../../utils/format'
import '../../styles/components/recommendation/PopularRecipes.css'

/**
 * 인기 레시피 순위 (대시보드 오른쪽 열).
 *
 * "요즘 남들은 뭘 해 먹나"를 보는 자리다. 왼쪽 열이 전부 AI가 골라준 것이라
 * 사람이 고른 결과를 나란히 놓아야 추천이 한쪽으로 치우쳤을 때 빠져나갈 데가 있다.
 *
 * 기간을 바꿀 수 있게 한 이유
 * --------------------------
 * 좋아요는 오래된 글일수록 쌓이기만 해서, 기간을 안 자르면 몇 달째 같은 순위가 뜬다.
 * 기본값을 30일로 두면 "요즘"이 되고, 오늘만 보면 실시간에 가까워진다.
 *
 * 지금은 화면에서 거르지만, 실제로는 서버가 기간을 받아
 * 그 안에서 눌린 좋아요만 세서(ORDER BY count DESC) 내려줘야 한다.
 */

/** 사이드에 보여줄 순위 개수. 전체는 /popular 에서 본다 */
const TOP_N = 8

const PRESETS = [
  { key: 'TODAY', label: '오늘', days: 0, title: '오늘' },
  { key: 'D7', label: '7일', days: 6, title: '오늘 -7일' },
  { key: 'D30', label: '30일', days: 29, title: '오늘 -30일' },
]

export default function PopularRecipes() {
  const [preset, setPreset] = useState('D30')
  const [range, setRange] = useState(() => ({
    start: toDateKey(shiftDays(-29)),
    end: toDateKey(new Date()),
  }))

  const choosePreset = (item) => {
    setPreset(item.key)
    setRange({ start: toDateKey(shiftDays(-item.days)), end: toDateKey(new Date()) })
  }

  // 날짜를 직접 고치면 어느 프리셋에도 해당하지 않는 기간이 된다.
  // 이때 탭이 '30일'에 켜진 채로 남으면 무엇을 보고 있는 건지 알 수 없다.
  const applyCustom = (start, end) => {
    setPreset('CUSTOM')
    setRange({ start, end })
  }

  const items = useMemo(
    () =>
      MOCK_LIKED_RECIPES
        // 'YYYY-MM-DD' 문자열끼리 비교하면 날짜순과 일치한다. 양 끝 포함.
        .filter((item) => {
          const key = toDateKey(item.liked_at)
          return key >= range.start && key <= range.end
        })
        // 좋아요 수가 같으면 최근에 받은 것을 위로 올린다.
        // 이 기준이 없으면 같은 데이터인데도 순서가 들쭉날쭉해 보인다.
        .sort(
          (a, b) =>
            b.like_count - a.like_count ||
            new Date(b.liked_at) - new Date(a.liked_at)
        )
        .slice(0, TOP_N),
    [range]
  )

  const rangeTitle =
    PRESETS.find((item) => item.key === preset)?.title ?? '직접 선택한 기간'

  return (
    <section className="card popular">
      <div className="section-head">
        <h2 className="section-title">
          <span aria-hidden="true">🔥</span> 인기 레시피{' '}
          <span className="popular__range">({rangeTitle})</span>
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
            className={`chip${preset === item.key ? ' is-active' : ''}`}
            onClick={() => choosePreset(item)}
            aria-pressed={preset === item.key}
          >
            {item.label}
          </button>
        ))}

        {/* 누르면 켜지기만 한다. 기간은 아래 입력칸에서 고른다 */}
        <button
          type="button"
          className={`chip${preset === 'CUSTOM' ? ' is-active' : ''}`}
          onClick={() => setPreset('CUSTOM')}
          aria-pressed={preset === 'CUSTOM'}
        >
          기간 선택 <span aria-hidden="true">📅</span>
        </button>
      </div>

      <DateRangeFilter start={range.start} end={range.end} onSearch={applyCustom} />

      <p className="popular__note muted">
        * 선택한 기간 동안 새로 받은 좋아요 수 기준
      </p>

      {items.length === 0 ? (
        <p className="empty">이 기간에 좋아요를 받은 레시피가 없습니다.</p>
      ) : (
        <ol className="popular__list">
          {items.map((item, i) => (
            <li key={item.id}>
              <Link
                to={`/recipe/${encodeURIComponent(item.food_name)}`}
                className="popular__row"
              >
                <span className="popular__rank">{i + 1}</span>
                <FoodThumb item={item} size="md" />
                <span className="popular__body">
                  <span className="popular__name">{item.recipe_name}</span>
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
