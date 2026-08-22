import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import WeatherCard from '../components/weather/WeatherCard'
import RecommendHero from '../components/recommendation/RecommendHero'
import FoodCard from '../components/recommendation/FoodCard'
import LikedList from '../components/recommendation/LikedList'
import DateRangeFilter from '../components/common/DateRangeFilter'
import FoodThumb from '../components/common/FoodThumb'
import {
  MOCK_HISTORIES,
  MOCK_LIKED_RECIPES,
  MOCK_RECOMMENDATIONS,
  MOCK_WEATHER,
} from '../mocks'
import { formatDate, formatPrice, shiftDays, toDateKey } from '../utils/format'
import '../styles/pages/HomePage.css'

/**
 * Dashboard. 별도 Landing Page 없이 접속하자마자 이 화면이 보인다(§54).
 *
 * 웹 모드에서는 좌우 2단(추천·기록 / 좋아요·안내),
 * 앱 모드에서는 위에서 아래로 한 줄로 쌓인다.
 *
 * 지금은 mocks/의 하드코딩 데이터를 쓴다.
 * 기간 필터도 화면에서 처리하지만, 실제로는 서버가 걸러서 내려줄 부분이다.
 */
export default function HomePage() {
  // 기본값: 오늘 포함 최근 30일
  // useState에 함수를 넘기면 첫 렌더에서 한 번만 계산된다.
  const [range, setRange] = useState(() => ({
    start: toDateKey(shiftDays(-29)),
    end: toDateKey(new Date()),
  }))

  const likedRecipes = useMemo(
    () =>
      MOCK_LIKED_RECIPES.filter((item) => {
        // 'YYYY-MM-DD' 문자열끼리 비교하면 날짜순과 일치한다. 양 끝 포함.
        const key = toDateKey(item.liked_at)
        return key >= range.start && key <= range.end
      }),
    [range]
  )

  // 대시보드에는 3건만. 전체는 /recommend 에서 본다
  const recommendations = MOCK_RECOMMENDATIONS.slice(0, 3)

  return (
    <div className="dash">
      {/* ===== 왼쪽 열 ===== */}
      <div className="dash__left">
        <div className="dash__top">
          <WeatherCard weather={MOCK_WEATHER} />
          <RecommendHero />
        </div>

        <section>
          <div className="section-head">
            <h2 className="section-title">오늘의 추천 레시피</h2>
            <Link to="/recommend" className="section-more">
              더보기 ›
            </Link>
          </div>

          <div className="dash__cards">
            {recommendations.map((food) => (
              <FoodCard key={food.food_name} food={food} />
            ))}
          </div>
        </section>

        <section>
          <div className="section-head">
            <h2 className="section-title">최근 기록</h2>
            <Link to="/history" className="section-more">
              더보기 ›
            </Link>
          </div>

          <div className="card dash__table-card">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>날짜</th>
                    <th>음식</th>
                    <th>레시피</th>
                    <th style={{ textAlign: 'right' }}>예상 재료비</th>
                    <th aria-label="상세" />
                  </tr>
                </thead>
                <tbody>
                  {MOCK_HISTORIES.map((item) => (
                    <tr key={item.id}>
                      <td className="muted">{formatDate(item.selected_at)}</td>
                      <td>
                        <span className="dash__food">
                          <FoodThumb item={item} size="sm" />
                          {item.food_name}
                        </span>
                      </td>
                      <td className="muted">{item.recipe_name}</td>
                      <td className="num price">{formatPrice(item.estimated_cost)}</td>
                      <td className="num">
                        <Link
                          to={`/recipe/${encodeURIComponent(item.food_name)}`}
                          className="dash__chevron"
                          aria-label={`${item.food_name} 상세`}
                        >
                          ⌄
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      {/* ===== 오른쪽 열 ===== */}
      <div className="dash__right">
        <section className="card">
          <div className="section-head">
            <h2 className="section-title">좋아요 받은 레시피</h2>
            <Link to="/liked" className="section-more">
              더보기 ›
            </Link>
          </div>

          <DateRangeFilter
            start={range.start}
            end={range.end}
            onSearch={(start, end) => setRange({ start, end })}
          />

          <LikedList items={likedRecipes} />
        </section>

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
