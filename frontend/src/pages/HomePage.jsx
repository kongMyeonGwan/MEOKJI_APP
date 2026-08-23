import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import WeatherPick from '../components/recommendation/WeatherPick'
import GamePromo from '../components/recommendation/GamePromo'
import FoodFinder from '../components/recommendation/FoodFinder'
import BudgetPicker from '../components/recommendation/BudgetPicker'
import FoodCard from '../components/recommendation/FoodCard'
import PopularRecipes from '../components/recommendation/PopularRecipes'
import CategoryBar from '../components/common/CategoryBar'
import { MOCK_CATEGORIES, MOCK_RECOMMENDATIONS } from '../mocks'
import '../styles/pages/HomePage.css'

/**
 * Dashboard. 별도 Landing Page 없이 접속하자마자 이 화면이 보인다(§54).
 *
 * 블록 순서에 의도가 있다.
 *
 *   ① 오늘 날씨 추천 (WeatherPick)  — 답. 들어오자마자 음식이 이미 정해져 있다
 *   ② 메뉴 게임 (GamePromo)         — 그게 안 당길 때, 고르는 걸 대신 해준다
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
 * 지금은 mocks/의 하드코딩 데이터를 쓴다.
 * 카테고리 필터도 화면에서 처리하지만, 실제로는 서버가 걸러서 내려줄 부분이다.
 */

/** 추천 카드 줄 수. 가로 스크롤이라 너무 많으면 끝까지 안 본다 */
const RECOMMEND_COUNT = 5

export default function HomePage() {
  const [category, setCategory] = useState('ALL')

  // CategoryBar는 key('KR')로 고르지만, 추천 데이터의 category와 /recommend 쿼리는
  // 라벨('한식')을 쓴다. 그 경계를 여기서 한 번만 넘긴다.
  // '전체'는 필터가 없다는 뜻이므로 라벨 대신 null로 둔다.
  const categoryLabel = useMemo(
    () =>
      category === 'ALL'
        ? null
        : (MOCK_CATEGORIES.find((c) => c.key === category)?.label ?? null),
    [category]
  )

  const recommendations = useMemo(
    () =>
      (categoryLabel
        ? MOCK_RECOMMENDATIONS.filter((f) => f.category === categoryLabel)
        : MOCK_RECOMMENDATIONS
      ).slice(0, RECOMMEND_COUNT),
    [categoryLabel]
  )

  // 고른 카테고리를 그대로 들고 넘어간다.
  // 안 그러면 "한식"을 고른 채 더보기를 눌렀을 때 전체 목록이 나와 선택이 풀린 것처럼 보인다.
  const moreHref = categoryLabel
    ? `/recommend?category=${encodeURIComponent(categoryLabel)}`
    : '/recommend'

  return (
    <div className="dash">
      {/* ===== 왼쪽 열 ===== */}
      <div className="dash__left">
        {/* ① 답 — 오늘 날씨에 맞는 음식이 이미 정해져 있다 */}
        <WeatherPick />

        {/* ② 그래도 못 정했을 때 */}
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

          {recommendations.length === 0 ? (
            <p className="empty">이 카테고리에는 아직 추천이 없어요.</p>
          ) : (
            <div className="dash__cards">
              {recommendations.map((food) => (
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
