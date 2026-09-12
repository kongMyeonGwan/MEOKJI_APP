import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../common/Icon'
import FoodThumb from '../common/FoodThumb'
import { getRecommendations } from '../../api/recommendations'
import { useAiLog } from '../../contexts/AiLogContext'
import { formatMinutes, formatPrice, priceBand } from '../../utils/format'
import '../../styles/components/recommendation/WeatherPick.css'

/**
 * 대시보드 맨 위 카드 — "오늘 날씨엔 이거".
 *
 * 이 화면의 1순위는 들어오자마자 오늘 먹을 게 이미 정해져 있는 것이다.
 * 그래서 왼쪽에서 오른쪽으로 읽으면 한 문장이 되도록 두 칸으로 나눴다.
 *
 *   왼쪽   28도 맑음, 덥고 담백한 게 어울려요    (근거)
 *   오른쪽  그래서 콩국수                        (답)
 *
 * 날씨와 음식을 다른 카드에 두면 사용자가 둘을 직접 이어붙여야 한다.
 * 한 덩어리로 묶어야 설명 없이 전달된다.
 *
 * 요청을 한 번만 보내는 이유
 * --------------------------
 * POST /api/recommendations 에 use_weather 를 켜면 서버가 날씨를 조회해
 * 그 날씨에 맞는 메뉴와 날씨 정보를 **한 응답에** 담아준다.
 * 날씨와 추천을 따로 부르면 화면이 두 번 나뉘어 뜨고, 서버와 화면이
 * 서로 다른 날씨를 보게 될 수도 있다.
 *
 * 5개가 쉬지 않고 흐른다
 * ---------------------
 * 전에는 5초마다 한 장씩 뛰었다. 뛰는 순간에만 움직임이 보여서, 그 사이에는
 * 멈춘 화면처럼 보였다. 지금은 CSS 애니메이션으로 계속 천천히 흐른다.
 *
 * 끊기지 않게 하려고 같은 5개를 한 벌 더 그린다. 줄 전체를 왼쪽으로 정확히
 * 절반(-50%) 옮기면 두 번째 벌의 첫 장이 첫 번째 벌의 첫 장 자리에 딱 온다.
 * 그 순간 처음으로 되돌려도 눈에는 이어져 보인다.
 *
 * 마우스를 올리면 멈춘다. 흐르는 것을 누르려다 놓치면 화가 난다.
 *
 * 5개를 가로로 늘어놓는다
 * ----------------------
 * 처음에는 하나씩만 보여줬다. "정해줬다" 는 인상을 지키려던 것인데, 넓은
 * 화면에서 오른쪽이 크게 비어 보였다. 그래서 가로로 깔고 스크롤한다.
 *
 * 대신 첫 장이 카드 폭을 거의 다 쓰도록 잡았다. 둘째 장이 오른쪽 끝에
 * 살짝 걸쳐 보이면 "옆에 더 있다" 는 것만 알려주고, 여전히 눈에 들어오는
 * 것은 하나다. scroll-snap 이 항상 한 장에 맞춰 세운다.
 */

/** 날씨 코드별 배경 CSS 클래스. condition_text(한글)가 아니라 코드로 나눈다 */
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

/** 한 번에 받아올 후보 개수 */
const PAGE_SIZE = 5

/** 위치 권한 응답을 기다리는 시간(밀리초). 넘으면 좌표 없이 요청한다 */
const GEO_TIMEOUT = 5000

/**
 * 브라우저에서 현재 좌표를 받아온다. 실패하면 null.
 *
 * 권한을 거부하거나 지원하지 않는 브라우저여도 카드가 안 뜨면 안 된다.
 * 좌표가 null 이면 서버가 기본 좌표(종로2가)로 대답한다.
 */
function getCoords() {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve(null)
      return
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          lat: position.coords.latitude,
          lon: position.coords.longitude,
        })
      },
      () => resolve(null),
      { timeout: GEO_TIMEOUT },
    )
  })
}

export default function WeatherPick() {
  const [weather, setWeather] = useState(null)
  const [picks, setPicks] = useState([])

  // 새 후보를 받아오는 중이면 버튼을 잠근다
  const [loading, setLoading] = useState(true)

  // 흐름을 잠시 멈춘 상태 (마우스를 올렸거나 카드 안에 포커스가 있을 때)
  const [paused, setPaused] = useState(false)

  const { add } = useAiLog()

  // 위치 좌표는 한 번만 받아 놓고 '다른 메뉴 추천'에서도 다시 쓴다.
  // useRef 에 담는 이유는 값이 바뀌어도 화면을 다시 그릴 필요가 없어서다.
  const coords = useRef(null)

  // 이미 보여준 메뉴. 새로 뽑을 때 겹치지 않게 서버에 함께 보낸다.
  const shown = useRef([])

  /**
   * 서버에 후보를 요청한다.
   *
   * 실패해도 카드만 사라지고 대시보드의 나머지는 그대로다.
   * 날씨 API 가 죽었다고 화면 전체가 에러 페이지가 되면 안 된다.
   */
  function load() {
    setLoading(true)

    getRecommendations({
      source: '날씨 추천',
      limit: PAGE_SIZE,
      useWeather: true,
      lat: coords.current?.lat ?? null,
      lon: coords.current?.lon ?? null,
      exclude: shown.current,
    })
      .then((data) => {
        // 후보가 하나도 없으면 이전 목록을 그대로 둔다.
        // 빈 배열로 덮으면 잘 보이던 카드가 갑자기 사라진다.
        if (data.items.length === 0) {
          return
        }

        setWeather(data.weather)
        setPicks(data.items)
        const names = []
        for (const item of data.items) {
          names.push(item.food_name)
        }

        // 다음 '다른 메뉴 추천'에서 같은 게 또 나오지 않게 쌓아둔다
        shown.current = [...shown.current, ...names]

        add({ source: '날씨 추천', condition: data.condition, foods: names })
      })
      .catch(() => {
        // 서버가 안 떠 있는 경우. 카드를 그리지 않는다.
      })
      .finally(() => setLoading(false))
  }

  // --- 처음 한 번: 좌표를 받고 후보를 불러온다 ---
  useEffect(() => {
    let alive = true

    getCoords().then((position) => {
      if (!alive) return
      coords.current = position
      load()
    })

    return () => {
      alive = false
    }
    // eslint 규칙상 load 를 의존성에 넣어야 하지만, 여기서는 처음 한 번만
    // 실행되어야 한다. load 를 넣으면 렌더할 때마다 다시 요청이 나간다.
  }, [])

  // 데이터가 없을 때는 카드가 깨지느니 통째로 안 보이는 편이 낫다
  if (!weather || picks.length === 0) {
    return null
  }

  const emoji = CONDITION_EMOJI[weather.condition] ?? '🌤️'
  const backgroundClass = CONDITION_CLASS[weather.condition] ?? ''

  return (
    <section
      className={`card wpick ${backgroundClass}`}
      aria-label="오늘 날씨 추천"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      // 포커스는 위로 전달되므로 카드 안 어느 버튼을 잡아도 멈춘다.
      // 키보드로 넘기는 중에 자동으로 또 넘어가면 조작이 엇나간다.
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      {/* 5개를 통째로 갈아 끼운다. 카드 모서리에 둬서 넘기기 버튼과 안 섞이게 한다 */}
      <button
        type="button"
        className="wpick__refresh"
        onClick={load}
        disabled={loading}
        aria-label="다른 메뉴 추천"
      >
        <Icon name="refresh" size={15} />
        {/* 좁은 화면에서는 글자를 접고 아이콘만 남긴다 (CSS 에서 처리) */}
        <span className="wpick__refresh-label">
          {loading ? '고르는 중…' : '다른 메뉴 추천'}
        </span>
      </button>

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

      {/* ===== 오른쪽 : 답 =====
          같은 5개를 한 벌 더 그린다. 줄 전체를 -50% 옮기면 두 번째 벌의
          첫 장이 첫 번째 벌의 첫 장 자리에 정확히 온다. 그 순간 처음으로
          되돌려도 눈에는 끊김 없이 이어진다.
          두 번째 벌은 화면낭독기와 탭 이동에서 건너뛴다 — 같은 내용이 두 번
          읽히면 메뉴가 열 개인 줄 안다 */}
      <div className={`wpick__viewport${paused ? ' is-paused' : ''}`}>
        <ul className="wpick__track">
          {[...picks, ...picks].map((food, i) => {
            const copy = i >= picks.length
            const href = `/recipe/${encodeURIComponent(food.food_name)}`
            const priceTag = priceBand(food.estimated_cost)
            const cardTags = priceTag
              ? [...(food.tags ?? []), priceTag]
              : food.tags ?? []

            return (
              <li
                className="wpick__pick"
                key={`${food.food_name}-${i}`}
                aria-hidden={copy}
                inert={copy ? '' : undefined}
              >
                <Link to={href} className="wpick__thumb" tabIndex={-1} aria-hidden="true">
                  <FoodThumb item={food} size="lg" />
                </Link>

                <div className="wpick__body">
                  <span className="wpick__badge">AI가 추천했어요! ✨</span>

                  <p className="wpick__lead">{weather.pick_lead}</p>
                  <h1 className="wpick__name">{food.food_name}</h1>

                  <p className="wpick__meta-line">
                    예상 비용 <span className="price">{formatPrice(food.estimated_cost)}</span>
                    <span className="wpick__dot" aria-hidden="true" />
                    <Icon name="clock" size={15} className="wpick__meta-icon" />
                    {formatMinutes(food.cook_time_minutes)}
                    <span className="wpick__dot" aria-hidden="true" />
                    난이도 {food.difficulty}
                  </p>

                  {/* 왜 이 메뉴인지 한 줄 */}
                  {food.reason && <p className="muted wpick__reason">{food.reason}</p>}

                  <div className="wpick__actions">
                    <Link to={href} className="btn">
                      레시피 보기
                    </Link>
                  </div>

                  {cardTags.length > 0 && (
                    <ul className="wpick__tags">
                      {cardTags.map((tag) => (
                        <li key={tag}>#{tag}</li>
                      ))}
                    </ul>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      </div>
    </section>
  )
}
