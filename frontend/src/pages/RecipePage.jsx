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
import { useLikeButton } from '../hooks/useLikeButton'
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
  // 비로그인이면 하트를 눌렀을 때 로그인 화면으로 보낸다 (훅 안에서 처리).
  const { isLiked, onLike } = useLikeButton()
  const { markPicked } = useAiLog()
  const { reload } = useProfile()

  const [recipe, setRecipe] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [saveError, setSaveError] = useState('')

  const name = decodeURIComponent(foodName ?? '')
  const liked = isLiked(name)

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
    // 서버가 하는 일 순서대로 적는다. AI가 레시피를 짓고, 그 재료로 시세를 찾는다.
    // 진행률을 받아오지는 못해서 시간으로 넘기지만, 순서는 실제와 같다.
    //
    // 두 번째로 여는 레시피는 서버가 기억해둬서 2초 만에 끝난다.
    // 그때는 첫 문구만 잠깐 스쳐 지나간다.
    const steps = [
      `AI가 '${name}' 레시피를 만드는 중...`,
      '재료와 분량을 정리하는 중...',
      '시장에서 재료 가격을 찾는 중...',
      '조금만 더 기다려 주세요. AI가 붐비는 시간일 수 있어요.',
    ]

    return (
      <div className="recipe-page">
        <p className="muted recipe-page__back">
          <Link to="/">← 홈으로</Link>
        </p>

        <section className="card">
          <Loading steps={steps} />
          <RecipeSkeleton />
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
  //
  // 쪼개 팔지 않는 재료(무 0.2개)는 한 단위를 통째로 산 값으로 들어간다.
  // 레시피가 요구하는 분량보다 큰 숫자라서, 왜 큰지 밝혀두지 않으면
  // 계산이 틀린 것처럼 읽힌다.
  const priceRange = getPriceSummary(recipe.prices)
  const partial = priceRange.counted < priceRange.total

  // 찜은 로그인 없이도 된다. 이 브라우저에만 저장되는 값이고(LikesContext),
  // "이걸로 먹을래요"와 달리 기록으로 남지 않는다

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
            {priceRange.minUnit > 0 && (
              <span className="muted recipe-page__cost-note">
                {priceRange.minUnit}개는 최소단위로 구입
              </span>
            )}
          </span>
        </header>

        <RecipeView
          recipe={recipe.recipe_json}
          ingredients={recipe.ingredients_json}
          substitutes={recipe.substitutes_json}
          // 시세를 못 구한 재료에 표시를 붙이려면 가격 결과가 필요하다
          prices={recipe.prices}
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
                onClick={() => onLike(name)}
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


/**
 * 레시피가 오기 전에 자리를 잡아두는 뼈대.
 *
 * 빈 카드에 글자 한 줄만 있으면 화면이 비어 보이고, 내용이 도착하는 순간
 * 레이아웃이 확 늘어나면서 한 번 튄다. 미리 같은 모양을 깔아두면
 * 기다리는 동안에도 "여기에 레시피가 들어오겠구나" 가 읽힌다.
 */
function RecipeSkeleton() {
  return (
    <div className="skeleton" aria-hidden="true">
      <div className="skeleton__bar skeleton__bar--title" />
      <div className="skeleton__bar skeleton__bar--sub" />

      <div className="skeleton__group">
        <div className="skeleton__bar" />
        <div className="skeleton__bar" />
        <div className="skeleton__bar skeleton__bar--short" />
      </div>

      <div className="skeleton__group">
        <div className="skeleton__bar" />
        <div className="skeleton__bar skeleton__bar--short" />
      </div>
    </div>
  )
}
