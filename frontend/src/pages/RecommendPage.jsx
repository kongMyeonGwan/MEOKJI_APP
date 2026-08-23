import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import FoodCard from '../components/recommendation/FoodCard'
import { MOCK_RECOMMENDATIONS, MOCK_WEATHER } from '../mocks'
import '../styles/pages/ListPage.css'

/**
 * AI 추천 전체 목록 (STEP 13).
 * 대시보드는 앞부분만 보여주고, 여기서 전부 본다.
 *
 * `?category=한식` 이 붙어 있으면 그 분류만 걸러서 보여준다.
 * 룰렛(Roulette)의 결과와 대시보드 카테고리 칩의 "더보기"가 이 쿼리로 들어온다.
 *
 * 쿼리에 key(KR)가 아니라 라벨(한식)을 담는 이유:
 * 추천 데이터의 `category` 필드가 라벨 문자열을 그대로 들고 있어서
 * 변환 계층이 필요 없고, 공유된 링크도 사람이 읽을 수 있다.
 * 서버가 붙으면 이 값을 그대로 쿼리 파라미터로 넘기면 된다.
 */
export default function RecommendPage() {
  const [params, setParams] = useSearchParams()
  const category = params.get('category')?.trim() || ''

  const results = useMemo(
    () =>
      category
        ? MOCK_RECOMMENDATIONS.filter((food) => food.category === category)
        : MOCK_RECOMMENDATIONS,
    [category]
  )

  // replace를 쓰는 이유는 필터를 껐다 켤 때마다 뒤로가기 기록이 쌓이지 않게 하기 위함.
  // 룰렛에서 들어온 사용자가 뒤로가기를 누르면 룰렛 화면으로 바로 돌아가야 한다.
  const clearCategory = () => setParams({}, { replace: true })

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

      {/* 왜 걸러졌는지 보이지 않으면 목록이 짧아진 이유를 알 수 없다 */}
      {category && (
        <div className="list-page__filter-bar">
          <span className="list-page__chip">
            {category}
            <button
              type="button"
              className="list-page__chip-clear"
              onClick={clearCategory}
              aria-label={`${category} 필터 해제`}
            >
              ×
            </button>
          </span>
          <span className="muted">{results.length}건</span>
        </div>
      )}

      {results.length === 0 ? (
        <p className="empty">
          {category} 추천을 찾지 못했습니다.
          <br />
          <button type="button" className="btn btn--ghost btn--sm" onClick={clearCategory}>
            전체 보기
          </button>
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
