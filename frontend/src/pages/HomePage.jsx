import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import WeatherPick from '../components/recommendation/WeatherPick'
import BargainPick from '../components/recommendation/BargainPick'
import GamePromo from '../components/recommendation/GamePromo'
import FoodFinder from '../components/recommendation/FoodFinder'
import BudgetPicker from '../components/recommendation/BudgetPicker'
import FoodCard from '../components/recommendation/FoodCard'
import PopularRecipes from '../components/recommendation/PopularRecipes'
import CategoryBar from '../components/common/CategoryBar'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import { getRecommendations } from '../api/recommendations'
import { useAiLog } from '../contexts/AiLogContext'
import { useCatalog } from '../contexts/CatalogContext'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/HomePage.css'

/**
 * Dashboard. 별도 Landing Page 없이 접속하자마자 이 화면이 보인다(§54).
 *
 * 블록 순서에 의도가 있다.
 *
 *   ① 오늘 날씨 추천 (WeatherPick)  — 답. 들어오자마자 음식이 이미 정해져 있다
 *   ② 지금 싼 재료로 (BargainPick)  — 날씨 말고 '돈'을 기준으로 고른 답
 *   ③ 메뉴 게임 (GamePromo)         — 둘 다 안 당길 때, 고르는 걸 대신해준다
 *   ③ 직접 찾기 (FoodFinder)        — 찾을 게 정해져 있을 때 쓰는 입력칸
 *   ④ 예산 설정 (BudgetPicker)      — 돈부터 정하고 싶을 때
 *   ⑤ 카테고리                      — 분류부터 고르고 싶을 때
 *   ⑥ 오늘의 추천 전체
 *
 * 이 화면의 1순위는 "오늘 날씨에 맞는 음식"을 보여주는 것이라 답이 맨 위로 온다.
 * 검색창과 카테고리를 위에 두면 화면이 "뭘 원하세요?"라고 묻는 꼴이 되어
 * 정해준다는 인상이 사라진다. 아래로 갈수록 사용자가 조건을 직접 좁히는 도구다.
 *
 * 오른쪽 열은 사람이 고른 결과(인기 레시피)다. 왼쪽이 전부 AI 추천이라
 * 취향이 안 맞을 때 빠져나갈 데를 옆에 둔다.
 *
 * 카테고리를 바꾸면 서버에 다시 물어본다. 받아놓고 화면에서 거르지 않는다.
 * 거르는 방식이면 '중식'을 골랐을 때 5개 중 한 개만 남아 목록이 텅 빈다.
 */

/** 추천 카드 개수. 가로 스크롤이라 너무 많으면 끝까지 안 본다 */
const RECOMMEND_COUNT = 5

export default function HomePage() {
  // 지금 고른 카테고리. 'ALL' 이면 전체
  const [category, setCategory] = useState('ALL')

  const [foods, setFoods] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const { categories } = useCatalog()
  const { add } = useAiLog()

  /**
   * 카테고리 키를 서버에 보낼 이름으로 바꾼다.
   *
   *   'KR'  -> '한식'
   *   'ALL' -> null   (전체는 거르지 않는다는 뜻)
   *
   * CategoryBar 는 key('KR')로 고르는데, 서버와 /recommend 주소는
   * 이름('한식')을 쓴다. 그 경계를 이 함수 한 곳에서만 넘긴다.
   */
  function getCategoryLabel(categoryKey) {
    if (categoryKey === 'ALL') {
      return null
    }

    const found = categories.find((item) => item.key === categoryKey)

    // find 는 못 찾으면 undefined 를 준다. ?. 는 "없으면 그냥 넘어가라" 는 뜻이다.
    return found?.label ?? null
  }

  const categoryLabel = getCategoryLabel(category)

  // 카테고리가 바뀔 때마다 서버에 다시 물어본다.
  // alive 플래그는 늦게 도착한 옛 응답이 새 결과를 덮지 않게 막는다.
  useEffect(() => {
    let alive = true
    setLoading(true)
    setLoadError('')

    getRecommendations({
      source: '카테고리',
      limit: RECOMMEND_COUNT,
      category: categoryLabel,
    })
      .then((data) => {
        if (!alive) return

        setFoods(data.items)

        const names = []
        for (const item of data.items) {
          names.push(item.food_name)
        }
        add({ source: '카테고리', condition: data.condition, foods: names })
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
    // add 는 매 렌더마다 새로 만들어지는 함수라 의존성에 넣으면 무한 루프가 된다.
    // 여기서 지켜봐야 할 값은 카테고리 하나뿐이다.
  }, [categoryLabel])

  // "더보기"를 누를 때 고른 카테고리를 그대로 들고 넘어간다.
  // 안 그러면 한식을 고른 채 더보기를 눌렀을 때 전체 목록이 나와서
  // 선택이 풀린 것처럼 보인다.
  let moreHref = '/recommend'
  if (categoryLabel) {
    moreHref = `/recommend?category=${encodeURIComponent(categoryLabel)}`
  }

  return (
    <div className="dash">
      {/* ===== 왼쪽 열 ===== */}
      <div className="dash__left">
        {/* ① 답 — 오늘 날씨에 맞는 음식이 이미 정해져 있다 */}
        <WeatherPick />

        {/* ② 같은 자리의 다른 축 — 날씨가 아니라 시세로 고른 답.
            싼 재료가 없는 날은 이 카드가 스스로 사라진다. */}
        <BargainPick />

        {/* ③ 그래도 못 정했을 때 */}
        <GamePromo />

        {/* ③ 찾을 게 정해져 있을 때 */}
        <FoodFinder />

        {/* ④ 돈부터 정하고 싶을 때 */}
        <BudgetPicker />

        {/* ⑤ 분류부터 고르고 싶을 때 */}
        <section>
          <h2 className="section-title dash__cats-title">카테고리</h2>
          <CategoryBar value={category} onChange={setCategory} />
        </section>

        <section>
          <div className="section-head">
            <h2 className="section-title">오늘의 추천 레시피</h2>
            <Link to={moreHref} className="section-more">
              더보기 ›
            </Link>
          </div>

          {loadError ? (
            <ErrorMessage message={loadError} />
          ) : loading ? (
            <Loading message="AI가 메뉴를 고르는 중..." />
          ) : foods.length === 0 ? (
            <p className="empty">이 카테고리에는 아직 추천이 없어요.</p>
          ) : (
            <div className="dash__cards">
              {foods.map((food) => (
                <FoodCard key={food.food_name} food={food} />
              ))}
            </div>
          )}
        </section>
      </div>

      {/* ===== 오른쪽 열 ===== */}
      <div className="dash__right">
        <PopularRecipes />

        <aside className="dash__tip">
          <span className="dash__tip-icon" aria-hidden="true">
            💡
          </span>
          <div>
            <h3 className="dash__tip-title">AI 추천은 참고용이에요</h3>
            <p className="dash__tip-body">
              알레르기나 특이 체질이 있으신 경우 반드시 재료를 확인하고
              섭취해주세요.
            </p>
          </div>
        </aside>
      </div>
    </div>
  )
}
