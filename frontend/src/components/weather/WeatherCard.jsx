import '../../styles/components/weather/WeatherCard.css'

/** 날씨 상태 코드 -> 아이콘. STEP 12에서 실제 API 코드에 맞춰 조정한다. */
const ICONS = {
  CLEAR: '☀️',
  CLOUDS: '☁️',
  RAIN: '🌧️',
  SNOW: '❄️',
  THUNDERSTORM: '⛈️',
}

export default function WeatherCard({ weather }) {
  if (!weather) return null

  return (
    <section className="card weather">
      <div className="weather__top">
        <span className="weather__icon" aria-hidden="true">
          {ICONS[weather.condition] ?? '🌤️'}
        </span>
        <span className="weather__temp">{weather.temp}°</span>
        <span className="weather__condition">{weather.condition_text}</span>
      </div>

      <p className="muted weather__detail">
        {weather.city} · 체감 {weather.feels_like}° · 습도 {weather.humidity}%
      </p>

      {weather.comment?.length > 0 && (
        <p className="weather__comment">
          {weather.comment.map((line) => (
            <span key={line}>{line}</span>
          ))}
        </p>
      )}
    </section>
  )
}
