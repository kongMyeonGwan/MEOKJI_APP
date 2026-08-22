import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import FoodCard from '../components/recommendation/FoodCard'
import { MOCK_RECOMMENDATIONS } from '../mocks'
import { formatPrice } from '../utils/format'
import '../styles/pages/ListPage.css'

const PRESETS = [5000, 8000, 10000, 15000]

/**
 * 예산 맞춤 추천.
 *
 * 예산을 쿼리스트링(?max=8000)에 둔다. 컴포넌트 state로만 들고 있으면
 * 새로고침하거나 링크를 공유했을 때 값이 사라진다.
 *
 * 지금은 화면에서 걸러내지만, 실제로는 AI 프롬프트에 상한을 넣어
 * 생성 단계에서 제한해야 한다. 만들어놓고 걸러내면 후보가 0건이 되기 쉽다.
 */
export default function BudgetPage() {
  const [params, setParams] = useSearchParams()
  const max = Number(params.get('max')) || 0

  const [draft, setDraft] = useState(max ? String(max) : '8000')

  const results = useMemo(
    () =>
      max === 0
        ? MOCK_RECOMMENDATIONS
        : MOCK_RECOMMENDATIONS.filter((food) => food.estimated_cost <= max),
    [max]
  )

  const apply = (amount) => {
    setDraft(String(amount))
    // replace를 쓰면 금액을 여러 번 바꿔도 뒤로가기 기록이 쌓이지 않는다
    setParams({ max: String(amount) }, { replace: true })
  }

  const onSubmit = (e) => {
    e.preventDefault()
    const amount = Number(draft)
    if (amount > 0) apply(amount)
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">예산 맞춤 추천</h2>
      </div>

      <div className="card list-page__filter">
        <form className="budget-form" onSubmit={onSubmit}>
          <input
            type="text"
            inputMode="numeric"
            className="field budget-form__amount"
            value={draft}
            onChange={(e) => setDraft(e.target.value.replace(/\D/g, ''))}
            maxLength={7}
            aria-label="예산 금액"
          />
          <span className="budget-form__unit">원 이하</span>
          <button type="submit" className="btn">
            추천받기
          </button>
        </form>

        <div className="budget-form__presets">
          {PRESETS.map((amount) => (
            <button
              key={amount}
              type="button"
              className={`budget-form__preset ${max === amount ? 'is-active' : ''}`}
              onClick={() => apply(amount)}
            >
              {amount >= 10000 ? `${amount / 10000}만원` : `${amount / 1000}천원`}
            </button>
          ))}
        </div>

        <p className="muted budget-form__hint">
          {max === 0
            ? '금액을 입력하면 그 안에서 만들 수 있는 레시피만 보여드립니다.'
            : `${formatPrice(max)} 이하 ${results.length}건`}
        </p>
      </div>

      {results.length === 0 ? (
        <p className="empty">
          {formatPrice(max)} 이하로 만들 수 있는 레시피를 찾지 못했습니다.
          <br />
          예산을 조금 올려보세요.
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
