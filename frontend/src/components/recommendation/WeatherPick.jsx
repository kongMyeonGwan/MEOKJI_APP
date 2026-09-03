import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../common/Icon'
import FoodThumb from '../common/FoodThumb'
import { fetchWeather } from '../../api/weather'
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
  // STEP 12: 날씨는 실제 API. 도착 전에는 카드를 비워둔다(아래 !weather 분기).
  const [weather, setWeather] = useState(null)

  useEffect(() => {
    // 화면을 떠난 뒤 응답이 와도 setState가 헛돌지 않게 막는다.
    let alive = true

    const load = (coords) => {
      fetchWeather(coords)
        .then((data) => {
          if (alive) setWeather(data)
        })
        .catch(() => {
          // 날씨 API가 죽었다고 카드 전체를 감추지 않고 mock으로 대체한다.
          if (alive) setWeather(MOCK_WEATHER)
        })
    }

    if (!navigator.geolocation) {
      load()
    } else {
      navigator.geolocation.getCurrentPosition(
        (pos) => load({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
        () => load(), // 권한 거부·실패 -> 좌표 없이 요청, 서버가 종로2가로 대답한다
        { timeout: 5000 }
      )
    }

    return () => {
      alive = false
    }
  }, [])

  const picks = MOCK_WEATHER_PICKS
  const pick = picks[index]
  // 음식 추천은 아직 mock이다(STEP 10/13에서 AI로 교체). 날씨 문구와 섞이지 않게 따로 둔다.
  const pickLead = MOCK_WEATHER.pick_lead

  // 데이터가 없을 때 카드가 깨지느니 통째로 빠지는 편이 낫다.
  // 날씨는 도착 전, picks는 STEP 10/13에서 AI 호출이 실패하면 실제로 이 상태가 된다.
  if (!weather || !pick) return null

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

        {/* 구·동까지 나오면 길어져서 체감·습도와 한 줄에 못 붙는다. 줄을 따로 준다 */}
        <p className="wpick__location muted">{weather.city}</p>

        <p className="wpick__meta muted">
          <span>체감 {weather.feels_like}°C</span>
          <span className="wpick__bar" aria-hidden="true" />
          <span>습도 {weather.humidity}%</span>
        </p>

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

          <p className="wpick__lead">{pickLead}</p>
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
