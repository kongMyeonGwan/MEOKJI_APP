import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import FoodCard from '../components/recommendation/FoodCard'
import Loading from '../components/common/Loading'
import BudgetRange, {
  BUDGET_MAX,
  BUDGET_MIN,
} from '../components/recommendation/BudgetRange'
import { getRecommendations } from '../api/recommendations'
import { useAiLog } from '../contexts/AiLogContext'
import { parseApiError } from '../utils/apiError'
import { formatPrice } from '../utils/format'
import '../styles/pages/ListPage.css'

// 예산 맞춤 추천 화면.
//
// 예산을 주소창(?min=8000&max=15000)에 둔다.
// useState 로만 들고 있으면 새로고침하거나 링크를 공유했을 때 값이 사라진다.
//
// 예산은 화면에서 거르지 않고 서버에 조건으로 넘긴다.
// 만들어놓고 걸러내면 후보가 0건이 되기 쉬워서 만들 때부터 제한해야 한다.

// 자주 쓰는 상한. 0원부터 이 금액까지를 한 번에 잡아준다.
const PRESETS = [5000, 8000, 10000, 15000]

// 한 번에 받아올 개수
const PAGE_SIZE = 12

// 주소에서 금액을 읽는다.
// 값이 아예 없을 때랑 0원일 때를 구분해야 한다.
// Number(params.get('max')) || 0 으로 쓰면 max 가 없을 때도 0 이 돼서
// "상한 0원" 이랑 "상한 없음" 이 같은 값이 된다.
function readAmount(params, key, fallback) {
  const raw = params.get(key)

  if (raw === null || raw.trim() === '') {
    return fallback
  }

  const amount = Number(raw)
  if (!Number.isFinite(amount)) {
    return fallback
  }

  // 손으로 고친 주소가 슬라이더 범위를 벗어나면 손잡이가 트랙 밖으로 나간다
  if (amount < BUDGET_MIN) {
    return BUDGET_MIN
  }
  if (amount > BUDGET_MAX) {
    return BUDGET_MAX
  }
  return amount
}

export default function BudgetPage() {
  const [params, setParams] = useSearchParams()

  const rawMin = readAmount(params, 'min', BUDGET_MIN)
  const rawMax = readAmount(params, 'max', BUDGET_MAX)

  // ?min=15000&max=5000 같은 주소가 들어와도 구간이 뒤집히지 않게 정리한다
  let min = rawMin
  let max = rawMax
  if (rawMin > rawMax) {
    min = rawMax
    max = rawMin
  }

  // 상한이 슬라이더 끝이면 "그 이상 전부" 라는 뜻이다.
  // 이 판단은 여기 한 곳에서만 한다.
  const unlimited = max >= BUDGET_MAX

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const { add } = useAiLog()

  // 예산이 바뀔 때마다 서버에 다시 물어본다.
  //
  // 장치가 두 개 있는데 둘 다 필요하다.
  //
  // 1. setTimeout 400ms
  //    슬라이더는 손잡이를 끄는 동안 1000원마다 값이 바뀐다.
  //    0원에서 2만원까지 한 번 끌면 요청이 스무 번 나간다.
  //    0.4초 안에 또 바뀌면 앞의 타이머를 취소하고 새로 잡는다.
  //
  // 2. alive
  //    요청이 보낸 순서대로 도착하지 않는다.
  //    8천원 응답이 1만원 응답보다 늦게 오면 엉뚱한 목록이 남는다.
  useEffect(() => {
    let alive = true
    setLoading(true)
    setLoadError('')

    const timer = setTimeout(function () {
      // 슬라이더가 끝까지 가 있으면 상한이 없다는 뜻이라 아예 안 보낸다
      let budgetMax = max
      if (unlimited) {
        budgetMax = null
      }

      getRecommendations({
        source: '예산 추천',
        limit: PAGE_SIZE,
        budgetMin: min,
        budgetMax: budgetMax,
      })
        .then(function (data) {
          if (!alive) {
            return
          }

          setItems(data.items)

          const names = []
          for (let i = 0; i < data.items.length; i++) {
            names.push(data.items[i].food_name)
          }

          add({ source: '예산 추천', condition: data.condition, foods: names })
        })
        .catch(function (err) {
          if (alive) {
            setLoadError(parseApiError(err).message)
          }
        })
        .finally(function () {
          if (alive) {
            setLoading(false)
          }
        })
    }, 400)

    // 예산이 또 바뀌거나 화면을 떠날 때 실행된다
    return function () {
      alive = false // 이미 보낸 요청의 응답을 무시한다
      clearTimeout(timer) // 아직 안 보낸 요청은 취소한다
    }
  }, [min, max, unlimited])

  // 슬라이더를 움직였을 때 주소를 바꾼다.
  // replace: true 를 주면 뒤로가기 기록이 안 쌓인다.
  // 없으면 슬라이더를 한 번 밀 때마다 기록이 하나씩 생긴다.
  function apply(nextMin, nextMax) {
    setParams({ min: String(nextMin), max: String(nextMax) }, { replace: true })
  }

  // 하한도 끝, 상한도 끝이면 사실상 조건이 없는 상태다
  const isWholeRange = min === BUDGET_MIN && unlimited

  let rangeText = formatPrice(min) + ' ~ ' + formatPrice(max)
  if (unlimited) {
    rangeText = formatPrice(min) + ' 이상'
  }

  let hintText = rangeText + ' ' + items.length + '건'
  if (isWholeRange) {
    hintText = '범위를 좁히면 그 안에서 만들 수 있는 레시피만 보여드립니다.'
  }

  let wholeClass = 'budget-form__preset'
  if (isWholeRange) {
    wholeClass = 'budget-form__preset is-active'
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">예산 맞춤 추천</h2>
      </div>

      <div className="card list-page__filter">
        <BudgetRange min={min} max={max} onChange={apply} />

        <div className="budget-form__presets">
          {PRESETS.map(function (amount) {
            let presetClass = 'budget-form__preset'
            if (min === BUDGET_MIN && max === amount) {
              presetClass = 'budget-form__preset is-active'
            }

            let label = amount / 1000 + '천원'
            if (amount >= 10000) {
              label = amount / 10000 + '만원'
            }

            return (
              <button
                key={amount}
                type="button"
                className={presetClass}
                onClick={() => apply(BUDGET_MIN, amount)}
              >
                {label} 이하
              </button>
            )
          })}

          <button
            type="button"
            className={wholeClass}
            onClick={() => apply(BUDGET_MIN, BUDGET_MAX)}
          >
            전체
          </button>
        </div>

        <p className="muted budget-form__hint">{hintText}</p>
      </div>

      {loadError && <ErrorMessage message={loadError} />}

      {!loadError && loading && (
        <Loading message="AI가 예산에 맞는 메뉴를 고르는 중..." />
      )}

      {!loadError && !loading && items.length === 0 && (
        <p className="empty">
          {rangeText}로 만들 수 있는 레시피를 찾지 못했습니다.
          <br />
          범위를 조금 넓혀보세요.
        </p>
      )}

      {!loadError && !loading && items.length > 0 && (
        <div className="list-page__cards">
          {items.map(function (food) {
            return <FoodCard key={food.food_name} food={food} />
          })}
        </div>
      )}
    </div>
  )
}
