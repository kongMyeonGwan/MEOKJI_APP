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
 * 버튼이 두 종류인데 하는 일이 다르다
 * -----------------------------------
 *   넘기기 (‹ › · 점)     받아온 5개 안에서 이동 (요청 없음)
 *   다른 메뉴 추천 (우상단) 서버에 새 후보 5개를 다시 요청
 *
 * 한 버튼이 둘 다 하면 "다음 거"인지 "새로 뽑기"인지 알 수 없다.
 * 자리도 갈라놨다. 넘기기는 내용 아래, 새로 뽑기는 카드 모서리다.
 *
 * 5개를 한꺼번에 늘어놓지 않고 하나씩 보여주는 이유:
 * 여러 개를 나란히 깔면 "골라보세요"가 되어버려서 정해줬다는 인상이 사라진다.
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
 * 자동으로 다음 후보로 넘어가는 간격 (밀리초).
 *
 * 3초면 설명을 다 읽기 전에 넘어가고, 10초면 넘어가는 줄도 모른다.
 * 카드에 마우스를 올리거나 안쪽 버튼에 포커스가 있으면 멈춘다 —
 * 읽고 있거나 뭔가 고르는 중이라는 뜻이기 때문이다.
 */
const AUTO_MS = 5000

/** 사용자가 "화면 움직임 줄이기"를 켜뒀는지 확인한다 */
function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

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

  // 지금 보여주는 후보가 picks 배열의 몇 번째인가
  const [index, setIndex] = useState(0)

  // 새 후보를 받아오는 중이면 버튼을 잠근다
  const [loading, setLoading] = useState(true)

  // 자동 넘김을 잠시 멈춘 상태 (마우스를 올렸거나 버튼에 포커스가 있을 때)
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
        setIndex(0)

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

  // --- 자동으로 다음 후보 보여주기 ---
  //
  // setInterval 이 아니라 setTimeout 을 index 에 걸어둔다.
  // 손으로 넘기면 index 가 바뀌면서 이 effect 가 다시 실행되고,
  // 타이머도 처음부터 다시 시작한다.
  // 그래서 방금 손으로 넘긴 화면이 1초 만에 또 넘어가는 일이 없다.
  useEffect(() => {
    if (paused) return
    if (picks.length < 2) return

    // 움직임을 줄여달라고 설정한 사용자에게는 자동으로 바꾸지 않는다.
    // 어지럼증이나 주의력 문제로 켜두는 설정이라 무시하면 안 된다.
    if (prefersReducedMotion()) return

    const timer = setTimeout(() => {
      // % picks.length 로 나눈 나머지를 쓰면 마지막 다음에 0번으로 돌아온다
      setIndex((prev) => (prev + 1) % picks.length)
    }, AUTO_MS)

    // effect 가 다시 실행되기 전이나 화면을 떠날 때 타이머를 정리한다.
    // 안 하면 타이머가 계속 쌓여서 점점 빨리 넘어간다.
    return () => clearTimeout(timer)
  }, [index, paused, picks.length])

  const pick = picks[index]

  // 데이터가 없을 때는 카드가 깨지느니 통째로 안 보이는 편이 낫다
  if (!weather || !pick) {
    return null
  }

  const emoji = CONDITION_EMOJI[weather.condition] ?? '🌤️'
  const backgroundClass = CONDITION_CLASS[weather.condition] ?? ''
  const recipeHref = `/recipe/${encodeURIComponent(pick.food_name)}`

  /** 이전(-1) 또는 다음(+1) 후보로 넘어간다 */
  function move(step) {
    setIndex((prev) => {
      // 0번에서 이전으로 가면 마지막으로 가야 하는데,
      // -1 % 5 는 -1 이라서 그냥 나머지만 쓰면 안 된다.
      // picks.length 를 한 번 더해주면 항상 0 이상이 된다.
      return (prev + step + picks.length) % picks.length
    })
  }

  // 가격은 바로 위 줄에 원 단위로 적혀 있어서,
  // 해시태그에는 "7천원대" 같은 대략의 구간만 넣는다.
  const band = priceBand(pick.estimated_cost)
  const tags = pick.tags ?? []
  const allTags = band ? [...tags, band] : tags

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
          key 를 걸어두면 후보가 바뀔 때마다 리액트가 이 부분을 새로 그린다.
          그래서 살짝 떠오르는 CSS 효과가 다시 실행되고, "넘어갔다"가 눈에 보인다 */}
      <div className="wpick__pick" key={pick.food_name}>
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

          {/* 왜 이 메뉴인지 한 줄. AI가 만들어준 문장이다 */}
          {pick.reason && <p className="muted wpick__reason">{pick.reason}</p>}

          <div className="wpick__actions">
            <Link to={recipeHref} className="btn">
              레시피 보기
            </Link>
          </div>

          {allTags.length > 0 && (
            <ul className="wpick__tags">
              {allTags.map((tag) => (
                <li key={tag}>#{tag}</li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* ===== 넘기기 ===== */}
      <div className="wpick__nav">
        <button
          type="button"
          className="wpick__arrow"
          onClick={() => move(-1)}
          aria-label="이전 메뉴"
        >
          ‹
        </button>

        <ul className="wpick__dots">
          {picks.map((food, i) => (
            <li key={food.food_name}>
              <button
                type="button"
                className={`wpick__dot-btn${i === index ? ' is-on' : ''}`}
                onClick={() => setIndex(i)}
                aria-current={i === index}
                // 점만 있으면 어디로 가는지 모른다. 이름을 붙여두면
                // 마우스를 올렸을 때와 스크린리더 양쪽에서 읽힌다
                aria-label={food.food_name}
                title={food.food_name}
              />
            </li>
          ))}
        </ul>

        <button
          type="button"
          className="wpick__arrow"
          onClick={() => move(1)}
          aria-label="다음 메뉴"
        >
          ›
        </button>

        <span className="wpick__count muted">
          {index + 1} / {picks.length}
        </span>
      </div>
    </section>
  )
}
