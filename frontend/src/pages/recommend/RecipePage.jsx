import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodThumb from '../../components/common/FoodThumb'
import Icon from '../../components/common/Icon'
import Loading from '../../components/common/Loading'
import RecipeView from '../../components/common/RecipeView'
import PriceTable, { getPriceSummary } from './components/PriceTable'
import { getRecipe } from '../../api/recommendations'
import { createHistory } from '../../api/histories'
import { getFavorites, toggleFavorite } from '../../api/favorites'
import { markPicked } from '../../utils/aiLog'
import { formatPrice } from '../../utils/format'

// 레시피 상세 (/recipe/김치찌개).
// AI 가 레시피를 만들고, 재료 가격도 같이 보여준다.
// "이걸로 먹을래요" 를 누르면 내 기록(History)에 저장된다.
//
// user 는 App.jsx 가 넘겨준 로그인한 사람. 로그인 안 했으면 null
export default function RecipePage({ user }) {
  const { foodName } = useParams() // 주소의 :foodName 부분
  const navigate = useNavigate()

  const [recipe, setRecipe] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [liked, setLiked] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [saveError, setSaveError] = useState('')

  // 화면이 열릴 때 (다른 음식으로 옮겼을 때도) 레시피를 받아온다
  useEffect(() => {
    const load = async () => {
      // 앞 음식의 결과가 남지 않게 처음 상태로 되돌린다
      setRecipe(null)
      setLoading(true)
      setError('')
      setSaved(false)
      setLiked(false)

      try {
        const data = await getRecipe(foodName)
        setRecipe(data)
      } catch (err) {
        // 서버가 보낸 오류 문장을 그대로 보여준다.
        if (err.response) {
          // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
          const detail = err.response.data.detail
          if (Array.isArray(detail)) {
            setError(detail[0].msg)
          } else {
            setError(detail || '요청을 처리하지 못했습니다.')
          }
        } else if (err.request) {
          // 백엔드를 안 켰거나 30초가 지난 경우
          setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
        } else {
          // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
          setError(err.message)
        }
      } finally {
        setLoading(false)
      }

      // 로그인했으면 찜해둔 음식인지 확인한다
      if (user) {
        try {
          const likes = await getFavorites()
          setLiked(likes.names.includes(foodName))
        } catch (err) {
          // 몰라도 하트가 빈 채로 보일 뿐이다
        }
      }
    }
    load()
  }, [foodName])

  // 찜하기
  const handleLike = async () => {
    if (!user) {
      navigate('/login')
      return
    }
    try {
      const result = await toggleFavorite(foodName)
      setLiked(result.favorited)
    } catch (err) {
      alert('찜하지 못했습니다.')
    }
  }

  // "이걸로 먹을래요" → 내 기록에 저장
  const handleConfirm = async () => {
    if (!user) {
      navigate('/login')
      return
    }

    setSaving(true)
    setSaveError('')
    try {
      await createHistory(recipe)
      setSaved(true)
      markPicked(recipe.foodName) // AI 추천 기록에도 "이걸로 정함" 표시
    } catch (err) {
      // 서버가 보낸 오류 문장을 그대로 보여준다.
      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          setSaveError(detail[0].msg)
        } else {
          setSaveError(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        setSaveError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
        setSaveError(err.message)
      }
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="recipe-page">
        <p className="muted recipe-page__back">
          <Link to="/">← 홈으로</Link>
        </p>
        <section className="card">
          <Loading
            steps={[
              `AI가 '${foodName}' 레시피를 만드는 중...`,
              '재료와 분량을 정리하는 중...',
              '시장에서 재료 가격을 찾는 중...',
              '조금만 더 기다려 주세요. AI가 붐비는 시간일 수 있어요.',
            ]}
          />
        </section>
      </div>
    )
  }

  if (error || !recipe) {
    return (
      <div className="recipe-page">
        <section className="card">
          <p className="empty">
            {error || '레시피를 만들지 못했습니다.'}
            <br />
            <Link to="/">← 홈으로</Link>
          </p>
        </section>
      </div>
    )
  }

  // 재료비 합계 (가장 싼 곳 ~ 가장 비싼 곳)
  const price = getPriceSummary(recipe.prices)

  return (
    <div className="recipe-page">
      <p className="muted recipe-page__back">
        <Link to="/">← 홈으로</Link>
      </p>

      <section className="card">
        {/* 요리 사진. public/images/food 에 있는 사진을 쓰고, 없으면 그림이 나온다 */}
        <div className="recipe-page__photo">
          <FoodThumb item={recipe} size="lg" />
        </div>

        <header className="recipe-page__head">
          <div>
            <h2 className="recipe-page__name">{recipe.foodName}</h2>
            <p className="muted recipe-page__sub">{recipe.recipeName}</p>
          </div>
          <span className="recipe-page__cost">
            재료비 {formatPrice(price.min)}
            <span className="recipe-page__cost-max"> ~ {formatPrice(price.max)}</span>
            {price.counted < price.total && (
              <span className="muted recipe-page__cost-note">
                재료 {price.total}개 중 {price.counted}개 기준
              </span>
            )}
          </span>
        </header>

        <RecipeView
          recipe={recipe.recipeJson}
          ingredients={recipe.ingredientsJson}
          substitutes={recipe.substitutesJson}
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
              <Link to="/history" className="btn">
                History 보러가기
              </Link>
              <Link to="/reviews/write" className="btn btn--ghost">
                레시피 공유
              </Link>
            </div>
          </>
        ) : (
          <>
            <p className="muted" style={{ margin: '0 0 12px' }}>
              이 레시피로 결정하면 기록에 남습니다.
              {!user && ' (로그인이 필요합니다)'}
            </p>
            <div className="recipe-page__actions">
              <button type="button" className="btn" onClick={handleConfirm} disabled={saving}>
                {saving ? '저장 중…' : '이걸로 먹을래요'}
              </button>
              <button type="button" className="btn btn--ghost recipe-page__like" onClick={handleLike}>
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
