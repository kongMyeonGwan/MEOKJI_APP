import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../../../components/common/Icon'
import FoodThumb from '../../../components/common/FoodThumb'
import { getRecommendations } from '../../../api/recommendations'
import { addLog, foodNames } from '../../../utils/aiLog'
import { formatMinutes, formatPrice, priceBand } from '../../../utils/format'
import { GEO_OPTIONS, weatherLocationAlert } from '../../../utils/geolocation'

// 첫 화면 맨 위 카드. 왼쪽에 오늘 날씨, 오른쪽에 날씨에 맞는 메뉴 5개.
// 메뉴 카드는 CSS 애니메이션으로 옆으로 천천히 흐르고, 마우스를 올리면 멈춘다.
// (끊김 없이 흐르게 하려고 같은 목록을 두 번 이어 붙여서 그린다)

// 날씨별 배경 클래스와 이모지
const WEATHER_CLASS = { CLEAR: 'wpick--clear', CLOUDS: 'wpick--clouds', RAIN: 'wpick--rain', SNOW: 'wpick--snow' }
const WEATHER_EMOJI = { CLEAR: '☀️', CLOUDS: '☁️', RAIN: '🌧️', SNOW: '❄️' }

export default function WeatherPick() {
  const [weather, setWeather] = useState(null)
  const [picks, setPicks] = useState([])
  const [loading, setLoading] = useState(true)
  const [paused, setPaused] = useState(false) // 마우스를 올리면 흐름을 멈춘다
  const [coords, setCoords] = useState(null) // 내 위치 { lat, lon }
  const [shown, setShown] = useState([]) // 이미 보여준 메뉴 이름 (다시 안 나오게)

  // 서버에 날씨 + 메뉴를 요청한다.
  //   position : 브라우저로 잡은 내 위치. null 이면 못 읽은 것이다.
  //   geoCode  : 못 읽은 까닭 (1=권한 거부, 2=실패, 3=시간 초과)
  const load = async (position, geoCode) => {
    setLoading(true)
    try {
      const data = await getRecommendations({
        source: '날씨 추천',
        limit: 5,
        useWeather: true,
        lat: position ? position.lat : null,
        lon: position ? position.lon : null,
        exclude: shown,
      })

      // 위치를 못 읽어서 서버가 기본 자리(종로2가) 날씨를 준 경우다.
      // 그 날씨를 그대로 보여주되, 내 동네가 아니라는 걸 알림창으로 알린다.
      // 알리지 않으면 부산에 있는 사람이 종로 날씨를 자기 동네로 믿는다.
      //
      // 주소를 골라둔 사람은 여기 안 걸린다. 서버가 ADDRESS 를 주기 때문에
      // 위치를 못 읽어도 고른 주소의 날씨가 그대로 온다.
      //
      // geoCode 가 있을 때만 띄운다. 아래 '다시 뽑기' 버튼도 이 함수를 부르는데,
      // 그때는 까닭을 안 넘기므로 같은 알림이 누를 때마다 다시 뜨지 않는다.
      if (geoCode && data.weather && data.weather.locationSource === 'DEFAULT') {
        alert(weatherLocationAlert(geoCode, data.weather.city))
      }

      if (data.items.length > 0) {
        const names = foodNames(data.items)
        setWeather(data.weather)
        setPicks(data.items || [])
        setShown([...shown, ...names])
        addLog('날씨 추천', data.condition, names)
      }
    } catch (err) {
      // 실패하면 카드를 안 보여준다
    } finally {
      setLoading(false)
    }
  }

  // 처음 한 번, 브라우저에 내 위치를 물어보고 메뉴를 불러온다
  useEffect(() => {
    navigator.geolocation.getCurrentPosition(
      // 위치를 받았을 때
      (position) => {
        const myCoords = { lat: position.coords.latitude, lon: position.coords.longitude }
        setCoords(myCoords)
        load(myCoords)
      },
      // 위치 권한을 거부했거나 실패했을 때. 까닭(code)을 같이 넘긴다
      (error) => load(null, error.code),
      GEO_OPTIONS,
    )
  }, [])

  if (!weather || picks.length === 0) {
    return null
  }

  const emoji = WEATHER_EMOJI[weather.condition] || '🌤️'

  return (
    <section
      className={`card wpick ${WEATHER_CLASS[weather.condition] || ''}`}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      {/* 메뉴 5개를 새로 받아오는 버튼 */}
      <button type="button" className="wpick__refresh" onClick={() => load(coords)} disabled={loading}>
        <Icon name="refresh" size={15} />
        <span className="wpick__refresh-label">{loading ? '고르는 중…' : '다른 메뉴 추천'}</span>
      </button>

      {/* 왼쪽: 날씨 */}
      <div className="wpick__weather">
        <p className="wpick__temp-row">
          <span className="wpick__emoji">{emoji}</span>
          <strong className="wpick__temp">
            {weather.temp}
            <span className="wpick__unit">°C</span>
          </strong>
          <span className="wpick__cond">{weather.conditionText}</span>
        </p>
        <p className="wpick__location muted">{weather.city}</p>
        <p className="wpick__meta muted">
          <span>체감 {weather.feelsLike}°C</span>
          <span className="wpick__bar" />
          <span>습도 {weather.humidity}%</span>
        </p>
        <p className="wpick__comment">
          {weather.comment} {emoji}
        </p>
      </div>

      {/* 오른쪽: 메뉴 (같은 목록을 두 번 이어 붙여서 끊김 없이 흐르게) */}
      <div className={paused ? 'wpick__viewport is-paused' : 'wpick__viewport'}>
        <ul className="wpick__track">
          {[...picks, ...picks].map((food, index) => {
            // 태그 목록 + 가격대 태그 (예: '7천원대')
            const tags = []
            if (food.tags) {
              food.tags.forEach((tag) => tags.push(tag))
            }
            const band = priceBand(food.estimatedCost)
            if (band) {
              tags.push(band)
            }

            return (
              <li className="wpick__pick" key={`${food.foodName}-${index}`}>
                <Link to={`/recipe/${food.foodName}`} className="wpick__thumb">
                  <FoodThumb item={food} size="lg" />
                </Link>

                <div className="wpick__body">
                  <span className="wpick__badge">레시피를 추천했어요! ✨</span>
                  <p className="wpick__lead">{weather.pickLead}</p>
                  <h1 className="wpick__name">{food.foodName}</h1>

                  <p className="wpick__meta-line">
                    예상 비용 <span className="price">{formatPrice(food.estimatedCost)}</span>
                    <span className="wpick__dot" />
                    <Icon name="clock" size={15} className="wpick__meta-icon" />
                    {formatMinutes(food.cookTimeMinutes)}
                    <span className="wpick__dot" />
                    난이도 {food.difficulty}
                  </p>

                  {food.reason && <p className="muted wpick__reason">{food.reason}</p>}

                  <div className="wpick__actions">
                    <Link to={`/recipe/${food.foodName}`} className="btn">
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
              </li>
            )
          })}
        </ul>
      </div>
    </section>
  )
}
