import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import DateRangeFilter from '../common/DateRangeFilter'
import { getPopular } from '../../api/posts'
import { shiftDays, toDateKey } from '../../utils/format'
import '../../styles/components/recommendation/PopularRecipes.css'

// 인기 레시피 순위 (첫 화면 오른쪽).
// 왼쪽은 전부 AI 가 골라준 거라 사람이 고른 결과를 나란히 놓았다.
//
// 기간을 바꿀 수 있게 한 이유는 좋아요가 오래된 글일수록 쌓이기만 해서
// 기간을 안 자르면 몇 달째 같은 순위가 뜨기 때문이다.
//
// 집계는 서버가 한다. 그 기간에 "새로" 눌린 좋아요만 세야 하는데
// 화면에서는 언제 눌렸는지 알 수 없다.

// 여기 보여줄 순위 개수. 전체는 /popular 에서 본다.
const TOP_N = 8

const PRESETS = [
  { key: 'TODAY', label: '오늘', days: 0, title: '오늘' },
  { key: 'D7', label: '7일', days: 6, title: '오늘 -7일' },
  { key: 'D30', label: '30일', days: 29, title: '오늘 -30일' },
]

// 오늘부터 며칠 전까지의 기간을 만든다. days 가 6 이면 최근 7일.
function makeStart(days) {
  return toDateKey(shiftDays(-days))
}

export default function PopularRecipes() {
  // 지금 켜진 탭. 'TODAY' / 'D7' / 'D30' / 'CUSTOM'
  const [preset, setPreset] = useState('D30')
  const [start, setStart] = useState(makeStart(29))
  const [end, setEnd] = useState(toDateKey(new Date()))

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  // 기간이 바뀔 때마다 서버에 다시 물어본다.
  //
  // alive 가 필요한 이유는 요청이 보낸 순서대로 도착하지 않기 때문이다.
  // '30일' 응답이 '오늘' 응답보다 늦게 오면 화면에 엉뚱한 목록이 남는다.
  // effect 가 다시 돌기 직전에 alive 가 false 가 되니까
  // 늦게 온 응답은 setItems 를 안 부른다.
  useEffect(() => {
    let alive = true
    setLoading(true)

    getPopular({ start: start, end: end, limit: TOP_N })
      .then(function (data) {
        if (alive) {
          setItems(data)
        }
      })
      .catch(function () {
        // 순위를 못 받았다고 첫 화면 전체가 에러가 되면 안 된다
        if (alive) {
          setItems([])
        }
      })
      .finally(function () {
        if (alive) {
          setLoading(false)
        }
      })

    return function () {
      alive = false
    }
  }, [start, end])

  // 오늘 / 7일 / 30일 탭을 눌렀을 때
  function choosePreset(item) {
    setPreset(item.key)
    setStart(makeStart(item.days))
    setEnd(toDateKey(new Date()))
  }

  // 날짜를 직접 골랐을 때.
  // 어느 탭에도 안 맞는 기간이 되니까 preset 을 'CUSTOM' 으로 바꾼다.
  // 탭이 '30일' 에 켜진 채로 남으면 지금 뭘 보고 있는지 알 수 없다.
  function applyCustom(nextStart, nextEnd) {
    setPreset('CUSTOM')
    setStart(nextStart)
    setEnd(nextEnd)
  }

  // 제목에 쓸 기간 이름. 직접 고른 기간이면 PRESETS 에 없다.
  let rangeTitle = '직접 선택한 기간'
  for (let i = 0; i < PRESETS.length; i++) {
    if (PRESETS[i].key === preset) {
      rangeTitle = PRESETS[i].title
      break
    }
  }

  let customClass = 'chip'
  if (preset === 'CUSTOM') {
    customClass = 'chip is-active'
  }

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
        {PRESETS.map(function (item) {
          let chipClass = 'chip'
          if (preset === item.key) {
            chipClass = 'chip is-active'
          }

          return (
            <button
              key={item.key}
              type="button"
              className={chipClass}
              onClick={() => choosePreset(item)}
            >
              {item.label}
            </button>
          )
        })}

        {/* 누르면 켜지기만 한다. 기간은 아래 입력칸에서 고른다. */}
        <button
          type="button"
          className={customClass}
          onClick={() => setPreset('CUSTOM')}
        >
          기간 선택 <span aria-hidden="true">📅</span>
        </button>
      </div>

      <DateRangeFilter start={start} end={end} onSearch={applyCustom} />

      <p className="popular__note muted">
        * 선택한 기간 동안 새로 받은 좋아요 수 기준
      </p>

      {loading && <p className="empty">불러오는 중…</p>}

      {!loading && items.length === 0 && (
        <p className="empty">이 기간에 좋아요를 받은 레시피가 없습니다.</p>
      )}

      {!loading && items.length > 0 && (
        <ol className="popular__list">
          {items.map(function (item, i) {
            let name = item.recipe_name
            if (!name) {
              name = item.food_name
            }

            return (
              <li key={item.id}>
                <Link
                  to={'/recipe/' + encodeURIComponent(item.food_name)}
                  className="popular__row"
                >
                  <span className="popular__rank">{i + 1}</span>
                  <FoodThumb item={item} size="md" />
                  <span className="popular__body">
                    <span className="popular__name">{name}</span>
                    <span className="popular__likes">♥ {item.like_count}</span>
                  </span>
                </Link>
              </li>
            )
          })}
        </ol>
      )}

      <Link to="/popular" className="btn btn--outline btn--block popular__all">
        인기 레시피 전체보기 →
      </Link>
    </section>
  )
}
