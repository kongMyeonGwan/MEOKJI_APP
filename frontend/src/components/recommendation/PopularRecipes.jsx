import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import DateRangeFilter from '../common/DateRangeFilter'
import { getPopular } from '../../api/posts'
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
 * 집계는 서버가 한다(GET /api/posts/popular).
 * 그 기간에 **새로 눌린** 좋아요만 세야 하는데, 화면에서는 언제 눌렸는지를
 * 알 수 없다. 목록에 실려 오는 건 이미 합쳐진 숫자뿐이기 때문이다.
 */

/** 사이드에 보여줄 순위 개수. 전체는 /popular 에서 본다 */
const TOP_N = 8

const PRESETS = [
  { key: 'TODAY', label: '오늘', days: 0, title: '오늘' },
  { key: 'D7', label: '7일', days: 6, title: '오늘 -7일' },
  { key: 'D30', label: '30일', days: 29, title: '오늘 -30일' },
]

/** 오늘부터 며칠 전까지의 기간을 만든다. days=6 이면 최근 7일. */
function makeRange(days) {
  return {
    start: toDateKey(shiftDays(-days)),
    end: toDateKey(new Date()),
  }
}

export default function PopularRecipes() {
  // 지금 켜진 탭 ('TODAY' / 'D7' / 'D30' / 'CUSTOM')
  const [preset, setPreset] = useState('D30')
  const [range, setRange] = useState(() => makeRange(29))

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  // 기간이 바뀔 때마다 서버에 다시 물어본다.
  //
  // alive 플래그가 필요한 이유: 요청은 보낸 순서대로 도착하지 않는다.
  // '30일' 응답이 '오늘' 응답보다 늦게 오면 화면에 엉뚱한 목록이 남는다.
  // effect 가 다시 실행되기 직전에 정리 함수가 alive 를 false 로 바꾸므로,
  // 낡은 응답이 도착해도 setItems 를 부르지 않는다.
  useEffect(() => {
    let alive = true
    setLoading(true)

    getPopular({ start: range.start, end: range.end, limit: TOP_N })
      .then((data) => {
        if (alive) setItems(data)
      })
      .catch(() => {
        // 순위를 못 받았다고 대시보드 전체가 에러가 되면 안 된다.
        if (alive) setItems([])
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [range.start, range.end])

  /** 오늘 / 7일 / 30일 탭을 눌렀을 때 */
  function choosePreset(item) {
    setPreset(item.key)
    setRange(makeRange(item.days))
  }

  /**
   * 날짜를 직접 골랐을 때.
   *
   * 어느 탭에도 해당하지 않는 기간이 되므로 preset 을 'CUSTOM' 으로 바꾼다.
   * 이때 탭이 '30일'에 켜진 채로 남으면 지금 뭘 보고 있는 건지 알 수 없다.
   */
  function applyCustom(start, end) {
    setPreset('CUSTOM')
    setRange({ start, end })
  }

  // 제목에 쓸 기간 이름. 직접 고른 기간이면 PRESETS 에 없다.
  const matchedPreset = PRESETS.find((item) => item.key === preset)
  const rangeTitle = matchedPreset?.title ?? '직접 선택한 기간'

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

      {loading ? (
        <p className="empty">불러오는 중…</p>
      ) : items.length === 0 ? (
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
                  <span className="popular__name">
                    {item.recipe_name ?? item.food_name}
                  </span>
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
