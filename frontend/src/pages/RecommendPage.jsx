import FoodCard from '../components/recommendation/FoodCard'
import { MOCK_RECOMMENDATIONS, MOCK_WEATHER } from '../mocks'
import '../styles/pages/ListPage.css'

/**
 * AI 추천 전체 목록 (STEP 13).
 * 대시보드는 3건만 보여주고, 여기서 전부 본다.
 */
export default function RecommendPage() {
  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">오늘 뭐 먹지?</h2>
        <span className="muted section-more">
          {MOCK_WEATHER.condition_text} · {MOCK_WEATHER.temp}° 기준
        </span>
      </div>

      <p className="muted list-page__desc">
        날씨와 등록하신 못 먹는 재료를 반영해 AI가 고른 메뉴입니다.
      </p>

      <div className="list-page__cards">
        {MOCK_RECOMMENDATIONS.map((food) => (
          <FoodCard key={food.food_name} food={food} />
        ))}
      </div>
    </div>
  )
}
