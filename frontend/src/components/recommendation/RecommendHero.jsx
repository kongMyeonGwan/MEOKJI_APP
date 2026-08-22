import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../../styles/components/recommendation/RecommendHero.css'

/**
 * 대시보드 상단 히어로.
 *
 * 세 가지 진입점을 한곳에 모은다.
 *   1) 먹고 싶은 음식을 직접 검색      -> /recipe/:foodName
 *   2) 예산 안에서 추천받기            -> /budget?max=8000
 *   3) AI에게 알아서 추천받기          -> /recommend
 *
 * 예산은 쿼리스트링으로 넘긴다. 그래야 새로고침하거나 링크를 공유해도
 * 같은 결과가 나온다.
 */
export default function RecommendHero() {
  const [keyword, setKeyword] = useState('')
  const [budget, setBudget] = useState('8000')
  const navigate = useNavigate()

  const onSearch = (e) => {
    e.preventDefault()
    const trimmed = keyword.trim()
    if (!trimmed) return
    navigate(`/recipe/${encodeURIComponent(trimmed)}`)
  }

  const onBudget = (e) => {
    e.preventDefault()
    const amount = Number(budget)
    if (!amount) return
    navigate(`/budget?max=${amount}`)
  }

  return (
    <section className="hero">
      <h1 className="hero__title">
        오늘 뭐 먹지? <span aria-hidden="true">✨</span>
      </h1>
      <p className="hero__subtitle">
        날씨, 예산, 못 먹는 재료를 고려해 AI가 딱 맞는 레시피를 추천해드릴게요.
      </p>

      <div className="hero__panels">
        <div className="hero__panel">
          <form onSubmit={onSearch}>
            <h3 className="hero__panel-title">원하는 음식을 검색해보세요</h3>
            <div className="hero__row">
              <input
                type="text"
                className="field"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="먹고 싶은 음식을 입력하세요 (예: 김치찌개)"
                maxLength={150}
                aria-label="음식 이름"
              />
              <button type="submit" className="btn">
                레시피 찾기
              </button>
            </div>
          </form>

          <form onSubmit={onBudget} className="hero__budget">
            <h3 className="hero__panel-title">예산으로 추천받기</h3>
            <div className="hero__row">
              <input
                type="text"
                inputMode="numeric"
                className="field hero__amount"
                value={budget}
                // 숫자만 남긴다. 콤마를 넣으면 입력 중 커서가 튄다
                onChange={(e) => setBudget(e.target.value.replace(/\D/g, ''))}
                maxLength={7}
                aria-label="예산 금액"
              />
              <span className="hero__unit">원 이하</span>
              <button type="submit" className="btn">
                예산 맞춤 추천
              </button>
            </div>
          </form>
        </div>

        <div className="hero__panel hero__panel--ai">
          <div className="hero__ai-text">
            <h3 className="hero__panel-title">AI 추천받기</h3>
            <p className="hero__ai-desc">
              오늘의 추천 메뉴를
              <br />
              AI가 제안해드려요!
            </p>
            <button
              type="button"
              className="btn btn--outline btn--sm"
              onClick={() => navigate('/recommend')}
            >
              추천받기
            </button>
          </div>

          {/* 로봇 마스코트. 이모지는 OS마다 모양이 달라 SVG로 그린다 */}
          <svg className="hero__ai-bot" viewBox="0 0 64 64" aria-hidden="true">
            <rect x="30" y="6" width="4" height="9" rx="2" fill="#c9bfb6" />
            <circle cx="32" cy="6" r="4" fill="#e8481c" />
            <rect x="10" y="15" width="44" height="34" rx="14" fill="#f3ede6" />
            <rect x="16" y="23" width="32" height="18" rx="9" fill="#2f2a27" />
            <circle cx="26" cy="32" r="4.2" fill="#fff" />
            <circle cx="38" cy="32" r="4.2" fill="#fff" />
            <rect x="4" y="27" width="7" height="13" rx="3.5" fill="#ded5cb" />
            <rect x="53" y="27" width="7" height="13" rx="3.5" fill="#ded5cb" />
          </svg>
        </div>
      </div>
    </section>
  )
}
