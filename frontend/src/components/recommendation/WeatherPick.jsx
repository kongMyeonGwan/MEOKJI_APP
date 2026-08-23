import { useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../common/Icon'
import FoodThumb from '../common/FoodThumb'
import { MOCK_WEATHER, MOCK_WEATHER_PICKS } from '../../mocks'
import { formatMinutes, formatPrice, priceBand } from '../../utils/format'
import '../../styles/components/recommendation/WeatherPick.css'

/**
 * 대시보드 히어로 — "오늘 날씨엔 이거".
 *
 * 이 화면의 1순위는 접속하자마자 **오늘 날씨에 맞는 음식이 이미 정해져 있는 것**이다.
 * 그래서 왼쪽에서 오른쪽으로 읽으면 하나의 문장이 되도록 두 칸으로 나눴다.
 *
 *   왼쪽  28° 맑음, 덥고 담백한 게 어울려요   (근거)
 *   오른쪽 그래서 콩국수                      (답)
 *
 * 날씨와 음식을 다른 카드에 두면 사용자가 인과를 직접 이어붙여야 한다.
 * 한 덩어리로 묶어야 설명 없이 전달된다.
 *
 * 후보를 5개 나란히 깔지 않는다. 여러 개를 같은 크기로 놓는 순간
 * "골라보세요"가 되어버려서 정해줬다는 느낌이 사라진다.
 * 대신 "다른 메뉴 추천"으로 한 번에 하나씩만 갈아 끼운다.
 */

/** 날씨별 배경. condition_text(한글 표시용)가 아니라 condition 코드로 분기한다 */
const CONDITION_CLASS = {
  CLEAR: 'wpick--clear',
  CLOUDS: 'wpick--clouds',
  RAIN: 'wpick--rain',
  SNOW: 'wpick--snow',
}

const CONDITION_EMOJI = {
  CLEAR: '☀️',
  CLOUDS: '☁️',
  RAIN: '🌧️',
  SNOW: '❄️',
}

export default function WeatherPick() {
  // 서버가 정렬해서 내려준 순서를 그대로 쓴다. 0번이 오늘의 한 접시.
  const [index, setIndex] = useState(0)

  const weather = MOCK_WEATHER
  const picks = MOCK_WEATHER_PICKS
  const pick = picks[index]

  // 데이터가 없을 때 카드가 깨지느니 통째로 빠지는 편이 낫다.
  // STEP 12/13에서 날씨 API나 AI 호출이 실패하면 실제로 이 상태가 된다.
  if (!pick) return null

  const nextPick = () => setIndex((i) => (i + 1) % picks.length)

  const recipeHref = `/recipe/${encodeURIComponent(pick.food_name)}`
  const emoji = CONDITION_EMOJI[weather.condition] ?? '🌤️'

  // 가격은 바로 위 줄에 원 단위로 적혀 있어서, 해시태그에는 대략의 구간만 넣는다
  const band = priceBand(pick.estimated_cost)
  const tags = band ? [...(pick.tags ?? []), band] : (pick.tags ?? [])

  return (
    <section
      className={`card wpick ${CONDITION_CLASS[weather.condition] ?? ''}`}
      aria-label="오늘 날씨 추천"
    >
      {/* ===== 왼쪽 : 근거 ===== */}
      <div className="wpick__weather">
        <p className="wpick__temp-row">
          <span className="wpick__emoji" aria-hidden="true">
            {emoji}
          </span>
          <strong className="wpick__temp">
            {weather.temp}
            <span className="wpick__unit">°C</span>
          </strong>
          <span className="wpick__cond">{weather.condition_text}</span>
        </p>

        <p className="wpick__meta muted">
          <span>{weather.city}</span>
          <span className="wpick__bar" aria-hidden="true" />
          <span>체감 {weather.feels_like}°C</span>
          <span className="wpick__bar" aria-hidden="true" />
          <span>습도 {weather.humidity}%</span>
        </p>

        {/* 서버가 줄바꿈으로 두 문장을 내려준다. 그대로 두 줄로 보여준다 */}
        <p className="wpick__comment">
          {weather.comment}
          <span aria-hidden="true"> {emoji}</span>
        </p>
      </div>

      {/* ===== 오른쪽 : 답 ===== */}
      <div className="wpick__pick">
        <Link to={recipeHref} className="wpick__thumb" tabIndex={-1} aria-hidden="true">
          <FoodThumb item={pick} size="lg" />
        </Link>

        <div className="wpick__body">
          <span className="wpick__badge">AI가 추천했어요! ✨</span>

          <p className="wpick__lead">{weather.pick_lead}</p>
          <h1 className="wpick__name">{pick.food_name}</h1>

          <p className="wpick__meta-line">
            예상 비용 <span className="price">{formatPrice(pick.estimated_cost)}</span>
            <span className="wpick__dot" aria-hidden="true" />
            <Icon name="clock" size={15} className="wpick__meta-icon" />
            {formatMinutes(pick.cook_time_minutes)}
            <span className="wpick__dot" aria-hidden="true" />
            난이도 {pick.difficulty}
          </p>

          {/* 별점은 있는 음식만 보여준다. 없으면 이 줄이 통째로 빠진다 */}
          {pick.rating && (
            <p className="stars wpick__rating">
              <Icon name="star" size={16} />
              {pick.rating.toFixed(1)}
              <span className="stars__count">
                ({pick.rating_count?.toLocaleString('ko-KR')})
              </span>
            </p>
          )}

          <div className="wpick__actions">
            <Link to={recipeHref} className="btn">
              레시피 보기
            </Link>
            <button type="button" className="btn btn--ghost" onClick={nextPick}>
              다른 메뉴 추천
              <Icon name="refresh" size={16} className="wpick__refresh" />
            </button>
          </div>

          {tags.length > 0 && (
            <ul className="wpick__tags">
              {tags.map((tag) => (
                <li key={tag}>#{tag}</li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
