import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../common/Icon'
import FoodThumb from '../common/FoodThumb'
import { getRecommendations } from '../../api/recommendations'
import { useAiLog } from '../../contexts/AiLogContext'
import { formatMinutes, formatPrice, priceBand } from '../../utils/format'
import '../../styles/components/recommendation/WeatherPick.css'

// 첫 화면 맨 위 카드. "오늘 날씨엔 이거" 를 보여준다.
//
// 왼쪽에 날씨(근거), 오른쪽에 메뉴(답) 를 둬서 왼쪽에서 오른쪽으로 읽으면
// 한 문장이 되게 했다. 둘을 다른 카드에 두면 사용자가 직접 이어붙여야 한다.
//
// POST /api/recommendations 에 use_weather 를 켜면 서버가 날씨랑 메뉴를
// 한 응답에 같이 준다. 따로 부르면 화면이 두 번 나뉘어 뜬다.
//
// 메뉴 5개가 CSS 애니메이션으로 계속 천천히 흐른다.
// 끊기지 않게 하려고 같은 5개를 한 벌 더 그린다.
// 줄 전체를 왼쪽으로 정확히 절반(-50%) 옮기면 두 번째 벌의 첫 장이
// 첫 번째 벌의 첫 장 자리에 딱 온다. 그때 처음으로 되돌리면 이어져 보인다.
//
// 마우스를 올리면 멈춘다. 흐르는 걸 누르려다 놓치면 짜증난다.

// 날씨 코드별 배경 클래스. 한글 대신 코드로 나눈다.
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

// 한 번에 받아올 메뉴 개수
const PAGE_SIZE = 5

// 위치 권한 응답을 기다리는 시간(밀리초). 넘으면 좌표 없이 요청한다.
const GEO_TIMEOUT = 5000

// 브라우저에서 지금 좌표를 받아온다. 실패하면 null 을 준다.
// 권한을 거부해도 카드는 떠야 하고, 좌표가 null 이면
// 서버가 기본 좌표(종로2가)로 대답해준다.
function getCoords() {
  return new Promise(function (resolve) {
    if (!navigator.geolocation) {
      resolve(null)
      return
    }

    navigator.geolocation.getCurrentPosition(
      function (position) {
        resolve({
          lat: position.coords.latitude,
          lon: position.coords.longitude,
        })
      },
      function () {
        resolve(null)
      },
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

  // 위치 좌표는 한 번만 받아두고 '다른 메뉴 추천' 에서도 다시 쓴다.
  // useRef 에 담은 이유는 값이 바뀌어도 화면을 다시 그릴 필요가 없어서다.
  const coords = useRef(null)

  // 이미 보여준 메뉴. 새로 뽑을 때 겹치지 않게 서버에 함께 보낸다.
  const shown = useRef([])

  // 서버에 메뉴를 요청한다.
  // 실패해도 이 카드만 안 보이고 첫 화면의 나머지는 그대로다.
  function load() {
    setLoading(true)

    let lat = null
    let lon = null
    if (coords.current) {
      lat = coords.current.lat
      lon = coords.current.lon
    }

    getRecommendations({
      source: '날씨 추천',
      limit: PAGE_SIZE,
      useWeather: true,
      lat: lat,
      lon: lon,
      exclude: shown.current,
    })
      .then(function (data) {
        // 메뉴가 하나도 없으면 원래 목록을 그대로 둔다.
        // 빈 배열로 덮으면 잘 보이던 카드가 갑자기 사라진다.
        if (data.items.length === 0) {
          return
        }

        setWeather(data.weather)
        setPicks(data.items)

        const names = []
        for (let i = 0; i < data.items.length; i++) {
          names.push(data.items[i].food_name)
        }

        // 다음에 '다른 메뉴 추천' 을 눌렀을 때 같은 게 또 안 나오게 쌓아둔다
        shown.current = shown.current.concat(names)

        add({ source: '날씨 추천', condition: data.condition, foods: names })
      })
      .catch(function () {
        // 서버가 안 떠 있는 경우. 카드를 안 그린다.
      })
      .finally(function () {
        setLoading(false)
      })
  }

  // 처음 한 번만. 좌표를 받고 메뉴를 불러온다.
  useEffect(() => {
    let alive = true

    getCoords().then(function (position) {
      if (!alive) {
        return
      }
      coords.current = position
      load()
    })

    return function () {
      alive = false
    }
    // load 를 의존성에 넣으면 렌더할 때마다 요청이 나간다
  }, [])

  // 데이터가 없을 때는 카드가 깨지느니 통째로 안 보이는 편이 낫다
  if (!weather || picks.length === 0) {
    return null
  }

  let emoji = CONDITION_EMOJI[weather.condition]
  if (!emoji) {
    emoji = '🌤️'
  }

  let backgroundClass = CONDITION_CLASS[weather.condition]
  if (!backgroundClass) {
    backgroundClass = ''
  }

  let refreshLabel = '다른 메뉴 추천'
  if (loading) {
    refreshLabel = '고르는 중…'
  }

  let viewportClass = 'wpick__viewport'
  if (paused) {
    viewportClass = 'wpick__viewport is-paused'
  }

  // 같은 목록을 한 벌 더 붙여서 끊김 없이 흐르게 만든다
  const doubled = picks.concat(picks)

  return (
    <section
      className={'card wpick ' + backgroundClass}
      aria-label="오늘 날씨 추천"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      // 포커스는 위로 전달돼서 카드 안 아무 버튼이나 잡아도 멈춘다.
      // 키보드로 넘기는 중에 자동으로 또 넘어가면 조작이 엇나간다.
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      {/* 5개를 통째로 갈아 끼우는 버튼 */}
      <button
        type="button"
        className="wpick__refresh"
        onClick={load}
        disabled={loading}
        aria-label="다른 메뉴 추천"
      >
        <Icon name="refresh" size={15} />
        {/* 좁은 화면에서는 글자를 접고 아이콘만 남긴다 (CSS 에서 처리) */}
        <span className="wpick__refresh-label">{refreshLabel}</span>
      </button>

      {/* 왼쪽 : 근거 */}
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

      {/* 오른쪽 : 답.
          두 번째 벌은 화면낭독기랑 탭 이동에서 건너뛴다.
          같은 내용을 두 번 읽으면 메뉴가 열 개인 줄 안다. */}
      <div className={viewportClass}>
        <ul className="wpick__track">
          {doubled.map(function (food, i) {
            // 뒤쪽 절반은 흐름을 이어붙이려고 한 번 더 그린 복사본이다
            const copy = i >= picks.length
            const href = '/recipe/' + encodeURIComponent(food.food_name)

            let cardTags = []
            if (food.tags) {
              cardTags = food.tags.slice()
            }

            const priceTag = priceBand(food.estimated_cost)
            if (priceTag) {
              cardTags.push(priceTag)
            }

            let inertValue = undefined
            if (copy) {
              inertValue = ''
            }

            return (
              <li
                className="wpick__pick"
                key={food.food_name + '-' + i}
                aria-hidden={copy}
              >
                <Link
                  to={href}
                  className="wpick__thumb"
                  tabIndex={-1}
                  aria-hidden="true"
                >
                  <FoodThumb item={food} size="lg" />
                </Link>

                <div className="wpick__body">
                  <span className="wpick__badge">AI가 추천했어요! ✨</span>

                  <p className="wpick__lead">{weather.pick_lead}</p>
                  <h1 className="wpick__name">{food.food_name}</h1>

                  <p className="wpick__meta-line">
                    예상 비용{" "}
                    <span className="price">
                      {formatPrice(food.estimated_cost)}
                    </span>
                    <span className="wpick__dot" aria-hidden="true" />
                    <Icon name="clock" size={15} className="wpick__meta-icon" />
                    {formatMinutes(food.cook_time_minutes)}
                    <span className="wpick__dot" aria-hidden="true" />
                    난이도 {food.difficulty}
                  </p>

                  {/* 왜 이 메뉴인지 한 줄 */}
                  {food.reason && (
                    <p className="muted wpick__reason">{food.reason}</p>
                  )}

                  <div className="wpick__actions">
                    <Link to={href} className="btn">
                      레시피 보기
                    </Link>
                  </div>

                  {cardTags.length > 0 && (
                    <ul className="wpick__tags">
                      {cardTags.map(function (tag) {
                        return <li key={tag}>#{tag}</li>
                      })}
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
