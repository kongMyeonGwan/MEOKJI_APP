import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import FoodCard from '../components/recommendation/FoodCard'
import BudgetRange, {
  BUDGET_MAX,
  BUDGET_MIN,
} from '../components/recommendation/BudgetRange'
import { MOCK_RECOMMENDATIONS } from '../mocks'
import { formatPrice } from '../utils/format'
import '../styles/pages/ListPage.css'

/** 자주 쓰는 상한. 하한 0원부터 그 금액까지를 한 번에 잡아준다 */
const PRESETS = [5000, 8000, 10000, 15000]

/**
 * 예산 맞춤 추천.
 *
 * 예산을 쿼리스트링(`?min=8000&max=15000`)에 둔다. 컴포넌트 state로만 들고 있으면
 * 새로고침하거나 링크를 공유했을 때 값이 사라진다.
 *
 * 대시보드의 "직접 찾기"(FoodFinder)와 **같은 BudgetRange 컴포넌트**를 쓴다.
 * 거기서 대충 범위를 잡고 들어와 여기서 미세조정하는 흐름이라,
 * 두 화면의 조작 방식이 다르면 값이 사라진 것처럼 보인다.
 *
 * 지금은 화면에서 걸러내지만, 실제로는 AI 프롬프트에 범위를 넣어
 * 생성 단계에서 제한해야 한다. 만들어놓고 걸러내면 후보가 0건이 되기 쉽다.
 */

/**
 * 쿼리에서 금액을 읽는다. 파라미터가 **없을 때**와 0원일 때를 구분해야 한다.
 *
 * `Number(params.get('max')) || 0` 으로 쓰면 max가 없을 때 0이 되어
 * "상한 0원"과 "상한 없음"이 같은 값이 된다. 슬라이더를 끝까지 밀면
 * 히어로가 max를 생략하던 시절에 전체 목록이 튀어나오던 원인이 이것이었다.
 */
function readAmount(params, key, fallback) {
  const raw = params.get(key)
  if (raw === null || raw.trim() === '') return fallback

  const amount = Number(raw)
  if (!Number.isFinite(amount)) return fallback

  // 손으로 고친 URL이 슬라이더 범위를 벗어나면 손잡이가 트랙 밖으로 나간다
  return Math.min(Math.max(amount, BUDGET_MIN), BUDGET_MAX)
}

export default function BudgetPage() {
  const [params, setParams] = useSearchParams()

  const rawMin = readAmount(params, 'min', BUDGET_MIN)
  const rawMax = readAmount(params, 'max', BUDGET_MAX)

  // `?min=15000&max=5000` 같은 URL이 들어와도 구간이 뒤집히지 않게 정렬한다
  const min = Math.min(rawMin, rawMax)
  const max = Math.max(rawMin, rawMax)

  // 상한이 슬라이더 끝이면 "그 이상 전부"라는 뜻이다.
  // 이 해석은 여기 한 곳에서만 한다. 보내는 쪽과 받는 쪽 양쪽에서 판단하면
  // 규칙이 어긋나는 순간 조용히 결과가 달라진다.
  const unlimited = max >= BUDGET_MAX

  const results = useMemo(
    () =>
      MOCK_RECOMMENDATIONS.filter(
        (food) =>
          food.estimated_cost >= min &&
          (unlimited || food.estimated_cost <= max)
      ),
    [min, max, unlimited]
  )

  // replace를 쓰면 슬라이더를 움직이는 동안 뒤로가기 기록이 쌓이지 않는다.
  // step이 1,000원이라 끝에서 끝까지 밀어도 20단계뿐이다.
  const apply = (nextMin, nextMax) =>
    setParams({ min: String(nextMin), max: String(nextMax) }, { replace: true })

  const isWholeRange = min === BUDGET_MIN && unlimited

  const rangeText = unlimited
    ? `${formatPrice(min)} 이상`
    : `${formatPrice(min)} ~ ${formatPrice(max)}`

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">예산 맞춤 추천</h2>
      </div>

      <div className="card list-page__filter">
        <BudgetRange min={min} max={max} onChange={apply} />

        <div className="budget-form__presets">
          {PRESETS.map((amount) => (
            <button
              key={amount}
              type="button"
              className={`budget-form__preset ${
                min === BUDGET_MIN && max === amount ? 'is-active' : ''
              }`}
              onClick={() => apply(BUDGET_MIN, amount)}
            >
              {amount >= 10000 ? `${amount / 10000}만원` : `${amount / 1000}천원`}
              {' 이하'}
            </button>
          ))}

          <button
            type="button"
            className={`budget-form__preset ${isWholeRange ? 'is-active' : ''}`}
            onClick={() => apply(BUDGET_MIN, BUDGET_MAX)}
          >
            전체
          </button>
        </div>

        <p className="muted budget-form__hint">
          {isWholeRange
            ? '범위를 좁히면 그 안에서 만들 수 있는 레시피만 보여드립니다.'
            : `${rangeText} ${results.length}건`}
        </p>
      </div>

      {results.length === 0 ? (
        <p className="empty">
          {rangeText}로 만들 수 있는 레시피를 찾지 못했습니다.
          <br />
          범위를 조금 넓혀보세요.
        </p>
      ) : (
        <div className="list-page__cards">
          {results.map((food) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
