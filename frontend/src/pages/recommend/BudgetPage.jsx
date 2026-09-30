import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodCard from '../../components/recommendation/FoodCard'
import Loading from '../../components/common/Loading'
import BudgetRange, { BUDGET_MAX, BUDGET_MIN } from '../../components/recommendation/BudgetRange'
import { getRecommendations } from '../../api/recommendations'
import { addLog, foodNames } from '../../utils/aiLog'
import { getErrorMessage } from '../../utils/apiError'
import { formatPrice } from '../../utils/format'
import '../../styles/pages/ListPage.css'

// 예산 맞춤 추천 (/budget?min=0&max=10000).
// 예산을 주소에 두어서 새로고침해도 그대로 남는다.

// 금액 버튼들 (0원 ~ 이 금액)
const PRESETS = [
  { amount: 5000, label: '5천원 이하' },
  { amount: 8000, label: '8천원 이하' },
  { amount: 10000, label: '1만원 이하' },
  { amount: 15000, label: '1만5천원 이하' },
]

export default function BudgetPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  // 주소에서 금액을 읽는다. 없으면 슬라이더 양 끝 값
  const min = Number(searchParams.get('min')) || BUDGET_MIN
  const max = Number(searchParams.get('max')) || BUDGET_MAX

  // 슬라이더를 끝까지 밀면 "상한 없음"
  const noLimit = max >= BUDGET_MAX

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 예산이 바뀔 때마다 추천을 다시 받아온다.
  // 슬라이더를 끄는 동안 값이 계속 바뀌어서, 0.4초 동안 안 바뀌었을 때만 요청한다.
  useEffect(() => {
    setLoading(true)
    setError('')

    const load = async () => {
      try {
        const data = await getRecommendations({
          source: '예산 추천',
          limit: 12,
          budgetMin: min,
          budgetMax: noLimit ? null : max,
        })
        setItems(data.items)
        addLog('예산 추천', data.condition, foodNames(data.items))
      } catch (err) {
        setError(getErrorMessage(err))
      } finally {
        setLoading(false)
      }
    }

    const timer = setTimeout(load, 400)

    // 0.4초 안에 값이 또 바뀌면 앞의 타이머를 취소한다
    return () => clearTimeout(timer)
  }, [min, max])

  // 예산을 바꾸면 주소를 바꾼다 → min, max 가 바뀌어서 위 useEffect 가 다시 돈다
  const changeBudget = (nextMin, nextMax) => {
    setSearchParams({ min: nextMin, max: nextMax })
  }

  const rangeText = noLimit ? `${formatPrice(min)} 이상` : `${formatPrice(min)} ~ ${formatPrice(max)}`

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">예산 맞춤 추천</h2>
      </div>

      <div className="card list-page__filter">
        <BudgetRange min={min} max={max} onChange={changeBudget} />

        <div className="budget-form__presets">
          {PRESETS.map((preset) => (
            <button
              key={preset.amount}
              type="button"
              className={min === 0 && max === preset.amount ? 'budget-form__preset is-active' : 'budget-form__preset'}
              onClick={() => changeBudget(BUDGET_MIN, preset.amount)}
            >
              {preset.label}
            </button>
          ))}

          <button
            type="button"
            className={min === 0 && noLimit ? 'budget-form__preset is-active' : 'budget-form__preset'}
            onClick={() => changeBudget(BUDGET_MIN, BUDGET_MAX)}
          >
            전체
          </button>
        </div>

        <p className="muted budget-form__hint">
          {rangeText} {items.length}건
        </p>
      </div>

      {loading && <Loading message="AI가 예산에 맞는 메뉴를 고르는 중..." />}
      {error && <ErrorMessage message={error} />}

      {!loading && !error && items.length === 0 && (
        <p className="empty">
          {rangeText}로 만들 수 있는 레시피를 찾지 못했습니다.
          <br />
          범위를 조금 넓혀보세요.
        </p>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="list-page__cards">
          {items.map((food) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
