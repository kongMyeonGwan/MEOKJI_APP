import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import RecipeView from '../components/recipe/RecipeView'
import PriceTable, { getPriceSummary } from '../components/price/PriceTable'
import { useAuth } from '../contexts/AuthContext'
import { MOCK_HISTORIES, MOCK_PRICES } from '../mocks'
import { formatPrice } from '../utils/format'
import '../styles/pages/RecipePage.css'

/**
 * AI가 생성한 레시피 상세 (STEP 11).
 *
 * 홈에서 음식을 검색하거나 추천 카드를 누르면 이 화면으로 온다.
 * "이걸로 먹을래요"를 눌러야 비로소 meal_histories에 저장된다(STEP 15).
 * 저장 전까지는 아무것도 DB에 남기지 않는다.
 */
export default function RecipePage() {
  const { foodName } = useParams()
  // 다른 음식으로 이동했을 때 "저장했습니다" 상태가 남지 않도록 새로 마운트한다.
  return <Recipe key={foodName} foodName={foodName} />
}

function Recipe({ foodName }) {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const [saved, setSaved] = useState(false)

  // 하드코딩 데이터에서 이름이 맞는 것을 찾고, 없으면 첫 번째를 보여준다.
  // 실제로는 AI 응답을 그대로 쓰게 된다.
  const decoded = decodeURIComponent(foodName ?? '')
  const source =
    MOCK_HISTORIES.find((h) => h.food_name === decoded) ?? MOCK_HISTORIES[0]

  // 쇼핑몰마다 값이 달라 재료비는 하나의 숫자가 아니라 구간으로 나온다.
  // 어디서 사느냐에 따라 얼마나 차이 나는지 먼저 보여준다.
  const priceRange = getPriceSummary(MOCK_PRICES)

  const onConfirm = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/recipe/${foodName}` } })
      return
    }
    setSaved(true)
  }

  return (
    <div className="recipe-page">
      <p className="muted recipe-page__back">
        <Link to="/">← 홈으로</Link>
      </p>

      <section className="card">
        <header className="recipe-page__head">
          <div>
            <h2 className="recipe-page__name">{decoded || source.food_name}</h2>
            <p className="muted recipe-page__sub">{source.recipe_name}</p>
          </div>
          <span className="recipe-page__cost">
            재료비 {formatPrice(priceRange.min)}
            <span className="recipe-page__cost-max">
              {' ~ '}
              {formatPrice(priceRange.max)}
            </span>
          </span>
        </header>

        <RecipeView
          recipe={source.recipe_json}
          ingredients={source.ingredients_json}
          substitutes={source.substitutes_json}
        />
      </section>

      <section className="card">
        <h3 className="section-title">재료 가격 비교</h3>
        <PriceTable prices={MOCK_PRICES} />
      </section>

      <section className="card recipe-page__confirm">
        {saved ? (
          <>
            <p className="recipe-page__done">기록에 저장했습니다.</p>
            <div className="recipe-page__actions">
              <button type="button" className="btn" onClick={() => navigate('/history')}>
                History 보러가기
              </button>
              <button
                type="button"
                className="btn btn--ghost"
                onClick={() => navigate('/community/write')}
              >
                커뮤니티에 공유
              </button>
            </div>
          </>
        ) : (
          <>
            <p className="muted" style={{ margin: '0 0 12px' }}>
              이 레시피로 결정하면 기록에 남습니다.
              {!isAuthenticated && ' (로그인이 필요합니다)'}
            </p>
            <button type="button" className="btn" onClick={onConfirm}>
              이걸로 먹을래요
            </button>
          </>
        )}
      </section>
    </div>
  )
}
