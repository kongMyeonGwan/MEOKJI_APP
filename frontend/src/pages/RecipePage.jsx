import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import Icon from '../components/common/Icon'
import Loading from '../components/common/Loading'
import RecipeView from '../components/recipe/RecipeView'
import PriceTable, { getPriceSummary } from '../components/price/PriceTable'
import { getRecipe } from '../api/recommendations'
import { createHistory } from '../api/histories'
import { useAiLog } from '../contexts/AiLogContext'
import { useAuth } from '../contexts/AuthContext'
import { useLikes } from '../contexts/LikesContext'
import { useProfile } from '../contexts/ProfileContext'
import { parseApiError } from '../utils/apiError'
import { formatPrice } from '../utils/format'
import '../styles/pages/RecipePage.css'

/**
 * AI가 생성한 레시피 상세 (STEP 11).
 *
 * 홈에서 음식을 검색하거나 추천 카드를 누르면 이 화면으로 온다.
 * "이걸로 먹을래요"를 눌러야 비로소 meal_histories에 저장된다(STEP 15).
 * 저장 전까지는 아무것도 DB에 남기지 않는다.
 *
 * 레시피와 재료 가격을 한 번에 받아온다(GET /api/recipes/{이름}).
 * 나눠서 부르면 레시피가 먼저 뜨고 가격표가 나중에 나타나 화면이 한 번 튄다.
 */
export default function RecipePage() {
  const { foodName } = useParams()
  // 다른 음식으로 이동했을 때 "저장했습니다" 상태가 남지 않도록 새로 마운트한다.
  return <Recipe key={foodName} foodName={foodName} />
}

function Recipe({ foodName }) {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const { has, toggle } = useLikes()
  const { markPicked } = useAiLog()
  const { reload } = useProfile()

  const [recipe, setRecipe] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [saveError, setSaveError] = useState('')

  const name = decodeURIComponent(foodName ?? '')

  // 화면이 열릴 때 AI에게 레시피를 만들어달라고 한다.
  // 응답이 몇 초 걸릴 수 있어서 로딩 문구를 확실히 띄운다.
  useEffect(() => {
    let alive = true

    getRecipe(name)
      .then((data) => {
        if (alive) setRecipe(data)
      })
      .catch((err) => {
        if (alive) setLoadError(parseApiError(err).message)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [name])

  /**
   * "이거 먹을래요" 버튼. 로그인 안 했으면 로그인 화면으로 보낸다.
   *
   * 저장에 성공하면 요리 레벨이 오르므로 프로필을 다시 불러온다.
   * 안 그러면 사이드바 레벨 카드가 새로고침 전까지 옛 값을 보여준다.
   */
  async function onConfirm() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/recipe/${foodName}` } })
      return
    }

    setSaving(true)
    setSaveError('')

    try {
      await createHistory(recipe)
      setSaved(true)

      // AI 추천 기록에도 "이걸로 정했다"고 표시한다
      markPicked(recipe.food_name)

      reload()
    } catch (err) {
      setSaveError(parseApiError(err).message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="recipe-page">
        <section className="card">
          <Loading message={`AI가 '${name}' 레시피를 만드는 중...`} />
        </section>
      </div>
    )
  }

  if (loadError || !recipe) {
    return (
      <div className="recipe-page">
        <section className="card">
          <p className="empty">
            {loadError || '레시피를 만들지 못했습니다.'}
            <br />
            <Link to="/">← 홈으로</Link>
          </p>
        </section>
      </div>
    )
  }

  // 시장마다 값이 달라 재료비는 하나의 숫자가 아니라 구간으로 나온다.
  // 어디서 사느냐에 따라 얼마나 차이 나는지 먼저 보여준다.
  //
  // KAMIS 는 농수산물만 조사하므로 가공식품은 값이 없다. 그래서 이 합계는
  // 재료 전체가 아닐 수 있다. 몇 개 기준인지 밝히지 않으면 사용자가
  // 이 숫자를 '이 요리 만드는 총비용'으로 읽는다.
  const priceRange = getPriceSummary(recipe.prices)
  const partial = priceRange.counted < priceRange.total

  // 찜은 로그인 없이도 된다. 이 브라우저에만 저장되는 값이고(LikesContext),
  // "이걸로 먹을래요"와 달리 기록으로 남지 않는다
  const liked = has(recipe.food_name)

  return (
    <div className="recipe-page">
      <p className="muted recipe-page__back">
        <Link to="/">← 홈으로</Link>
      </p>

      <section className="card">
        <header className="recipe-page__head">
          <div>
            <h2 className="recipe-page__name">{recipe.food_name}</h2>
            <p className="muted recipe-page__sub">{recipe.recipe_name}</p>
          </div>
          <span className="recipe-page__cost">
            재료비 {formatPrice(priceRange.min)}
            <span className="recipe-page__cost-max">
              {' ~ '}
              {formatPrice(priceRange.max)}
            </span>
            {partial && (
              <span className="muted recipe-page__cost-note">
                재료 {priceRange.total}개 중 {priceRange.counted}개 기준
              </span>
            )}
          </span>
        </header>

        <RecipeView
          recipe={recipe.recipe_json}
          ingredients={recipe.ingredients_json}
          substitutes={recipe.substitutes_json}
        />
      </section>

      <section className="card">
        <h3 className="section-title">재료 가격 비교</h3>
        <PriceTable prices={recipe.prices} />
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
            <div className="recipe-page__actions">
              <button
                type="button"
                className="btn"
                onClick={onConfirm}
                disabled={saving}
              >
                {saving ? '저장 중…' : '이걸로 먹을래요'}
              </button>
              <button
                type="button"
                className="btn btn--ghost recipe-page__like"
                onClick={() => toggle(recipe.food_name)}
                aria-pressed={liked}
              >
                <Icon name={liked ? 'heart-fill' : 'heart'} size={17} />
                {liked ? '찜 해제' : '찜하기'}
              </button>
            </div>
            <ErrorMessage message={saveError} />
          </>
        )}
      </section>
    </div>
  )
}
