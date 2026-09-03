import { useEffect, useMemo, useState } from 'react'
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
 * 조작이 두 가지인데 하는 일이 다르다
 * -----------------------------------
 *   넘기기(‹ › · 점)      오늘 뽑힌 **5개 안에서** 이동
 *   다른 메뉴 추천(우상단)  5개를 **통째로 새로 교체**
 *
 * 한 버튼이 두 역할을 겸하면 "다음 거"인지 "새로 뽑기"인지 알 수 없다.
 * 자리도 갈라놨다. 넘기기는 내용 아래, 새로 뽑기는 카드 모서리다.
 *
 * 후보 5개를 같은 크기로 늘어놓지 않고 한 번에 하나씩만 보여주는 이유:
 * 여러 개를 나란히 깔면 "골라보세요"가 되어버려서 정해줬다는 인상이 사라진다.
 * 넘기면서 보게 하면 화면에는 항상 한 접시만 있다.
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

/** 한 세트에 보여줄 후보 수 */
const PAGE_SIZE = 5

/**
 * 자동으로 넘어가는 간격.
 *
 * 3초면 설명을 다 읽기 전에 넘어가고, 10초면 넘어가는 줄 모른다.
 * 카드에 마우스를 올리거나 안쪽에 포커스가 있으면 멈춘다 —
 * 읽고 있거나 버튼을 고르는 중이라는 뜻이다.
 */
const AUTO_MS = 5000

export default function WeatherPick() {
  const pool = MOCK_WEATHER_PICKS

  // page: 몇 번째 5개 세트인가, index: 그 세트 안에서 몇 번째인가
  const [page, setPage] = useState(0)
  const [index, setIndex] = useState(0)
  const [paused, setPaused] = useState(false)

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

  const pageCount = Math.max(1, Math.ceil(pool.length / PAGE_SIZE))

  const picks = useMemo(() => {
    const slice = pool.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)
    // 마지막 세트가 5개보다 적으면 앞에서 끌어와 채운다.
    // 세트마다 개수가 다르면 아래 점 줄이 넘길 때마다 늘었다 줄었다 한다
    return slice.length === PAGE_SIZE
      ? slice
      : [...slice, ...pool.slice(0, PAGE_SIZE - slice.length)]
  }, [pool, page])

  // 자동 넘김.
  // setInterval이 아니라 setTimeout을 index에 걸어둔다. 손으로 넘기면 index가
  // 바뀌면서 타이머가 새로 시작되므로, 방금 넘긴 화면이 1초 만에 또 넘어가지 않는다.
  useEffect(() => {
    if (paused || picks.length < 2) return

    // 움직임을 줄여달라고 설정한 사용자에게는 자동으로 바꾸지 않는다.
    // 어지럼증·주의력 문제로 켜두는 설정이라 무시하면 안 된다
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    const timer = setTimeout(
      () => setIndex((i) => (i + 1) % picks.length),
      AUTO_MS
    )
    return () => clearTimeout(timer)
  }, [index, page, paused, picks.length])

  const pick = picks[index]
  // 음식 추천은 아직 mock이다(STEP 10/13에서 AI로 교체). 날씨 문구와 섞이지 않게 따로 둔다.
  const pickLead = MOCK_WEATHER.pick_lead

  // 데이터가 없을 때 카드가 깨지느니 통째로 빠지는 편이 낫다.
  // 날씨는 도착 전, picks는 STEP 10/13에서 AI 호출이 실패하면 실제로 이 상태가 된다.
  if (!weather || !pick) return null

  const emoji = CONDITION_EMOJI[weather.condition] ?? '🌤️'

  const move = (step) =>
    setIndex((i) => (i + step + picks.length) % picks.length)

  const refresh = () => {
    setPage((p) => (p + 1) % pageCount)
    // 새 세트는 1위부터 보여준다. 3번째를 보다가 새로 뽑았는데 또 3번째가
    // 나오면 "1등이 뭔지"를 놓친다
    setIndex(0)
  }

  const recipeHref = `/recipe/${encodeURIComponent(pick.food_name)}`

  // 가격은 바로 위 줄에 원 단위로 적혀 있어서, 해시태그에는 대략의 구간만 넣는다
  const band = priceBand(pick.estimated_cost)
  const tags = band ? [...(pick.tags ?? []), band] : (pick.tags ?? [])

  return (
    <section
      className={`card wpick ${CONDITION_CLASS[weather.condition] ?? ''}`}
      aria-label="오늘 날씨 추천"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      // 포커스는 버블링되므로 카드 안 어느 버튼을 잡아도 멈춘다.
      // 키보드로 넘기는 중에 자동으로 또 넘어가면 조작이 엇나간다
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      {/* 5개를 통째로 갈아 끼운다. 카드 모서리에 둬서 넘기기 버튼과 섞이지 않게 한다 */}
      <button
        type="button"
        className="wpick__refresh"
        onClick={refresh}
        aria-label="다른 메뉴 추천"
      >
        <Icon name="refresh" size={15} />
        {/* 좁은 화면에서는 글자를 접고 아이콘만 남긴다(CSS) */}
        <span className="wpick__refresh-label">다른 메뉴 추천</span>
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
          key를 걸어 후보가 바뀔 때마다 새로 그린다. 살짝 떠오르는 효과가
          다시 실행돼서 "넘어갔다"가 눈에 보인다 */}
      <div className="wpick__pick" key={pick.food_name}>
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
                // 점만 있으면 무엇으로 가는지 모른다. 이름을 붙여두면
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
