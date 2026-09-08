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

/** 자주 쓰는 상한. 하한 0원부터 그 금액까지를 한 번에 잡아준다 */
const PRESETS = [5000, 8000, 10000, 15000]

/** 한 번에 받아올 개수 */
const PAGE_SIZE = 12

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
 * 예산은 화면에서 거르지 않고 서버에 조건으로 넘긴다.
 * 만들어놓고 걸러내면 후보가 0건이 되기 쉬워서, 생성 단계에서 제한해야 한다.
 * 그래도 못 맞추면 서버가 예산을 조금 넓혀 가장 가까운 대안을 준다.
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

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const { add } = useAiLog()

  // 예산이 바뀔 때마다 서버에 다시 물어본다.
  //
  // 여기 두 가지 장치가 있는데 둘 다 필요하다.
  //
  // 1) setTimeout 400ms — 슬라이더는 손잡이를 끄는 동안 1000원마다 값이 바뀐다.
  //    0원에서 2만원까지 한 번 끌면 요청이 스무 번 나간다. AI 호출이라 더 아프다.
  //    0.4초 안에 또 바뀌면 앞의 타이머를 취소하고 새로 잡는다(아래 clearTimeout).
  //    손을 뗀 뒤에야 한 번 나간다.
  //
  // 2) alive 플래그 — 요청은 보낸 순서대로 도착하지 않는다.
  //    8천원 응답이 1만원 응답보다 늦게 오면 화면에 엉뚱한 목록이 남는다.
  useEffect(() => {
    let alive = true
    setLoading(true)
    setLoadError('')

    const timer = setTimeout(() => {
      getRecommendations({
        source: '예산 추천',
        limit: PAGE_SIZE,
        budgetMin: min,
        // 슬라이더가 끝까지 가 있으면 "상한 없음"이라 아예 안 보낸다
        budgetMax: unlimited ? null : max,
      })
        .then((data) => {
          if (!alive) return

          setItems(data.items)

          const names = []
          for (const item of data.items) {
            names.push(item.food_name)
          }
          add({ source: '예산 추천', condition: data.condition, foods: names })
        })
        .catch((err) => {
          if (alive) setLoadError(parseApiError(err).message)
        })
        .finally(() => {
          if (alive) setLoading(false)
        })
    }, 400)

    // 예산이 또 바뀌거나 화면을 떠날 때 실행된다
    return () => {
      alive = false        // 이미 보낸 요청의 응답을 무시한다
      clearTimeout(timer)  // 아직 안 보낸 요청은 취소한다
    }
  }, [min, max, unlimited])

  /**
   * 슬라이더를 움직였을 때 주소를 바꾼다.
   *
   * replace: true 를 주면 뒤로가기 기록이 쌓이지 않는다.
   * 이게 없으면 슬라이더를 한 번 밀 때마다 기록이 하나씩 생겨서,
   * 뒤로가기를 스무 번 눌러야 이전 화면으로 돌아간다.
   */
  function apply(nextMin, nextMax) {
    setParams(
      { min: String(nextMin), max: String(nextMax) },
      { replace: true },
    )
  }

  // 하한도 끝, 상한도 끝이면 사실상 필터가 없는 상태다
  const isWholeRange = min === BUDGET_MIN && unlimited

  let rangeText = `${formatPrice(min)} ~ ${formatPrice(max)}`
  if (unlimited) {
    rangeText = `${formatPrice(min)} 이상`
  }

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
            : `${rangeText} ${items.length}건`}
        </p>
      </div>

      {loadError ? (
        <ErrorMessage message={loadError} />
      ) : loading ? (
        <Loading message="AI가 예산에 맞는 메뉴를 고르는 중..." />
      ) : items.length === 0 ? (
        <p className="empty">
          {rangeText}로 만들 수 있는 레시피를 찾지 못했습니다.
          <br />
          범위를 조금 넓혀보세요.
        </p>
      ) : (
        <div className="list-page__cards">
          {items.map((food) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
