import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../../../components/common/FoodThumb'
import Loading from '../../../components/common/Loading'
import { getCatalog } from '../../../api/catalog'
import { getRecommendations } from '../../../api/recommendations'
import { addLog, foodNames } from '../../../utils/aiLog'
import { saveResult } from '../../../utils/gameResult'
import { formatMinutes, formatPrice } from '../../../utils/format'
import Confetti from './Confetti'

// 음식 추천 룰렛. 돌려서 나온 종류(한식, 중식...)에서 오늘의 한 끼를 하나 골라준다.
//
// 당첨되면
//   1. 폭죽이 터지고 '오늘의 한 끼!' 창이 뜬다
//   2. 그 분류에서 음식 3개를 추천받아, 첫 번째를 오늘의 한 끼로 크게 보여주고
//      나머지 둘은 '이런 메뉴도 있어요' 로 작게 보여준다
//   3. 창을 닫으면 판 아래에 결과 한 줄이 남는다
//
// 그리는 방법
//   CSS 의 conic-gradient 로 동그라미를 색깔별로 나눈다.
//   예) 조각이 4개면 한 조각이 90도
//       conic-gradient(빨강 0deg 90deg, 파랑 90deg 180deg, 노랑 180deg 270deg, 초록 270deg 360deg)
//
// 돌리는 방법
//   당첨 조각을 먼저 뽑고, 그 조각이 12시 바늘에 오도록 transform: rotate(각도) 로 돌린다.
//   5바퀴를 더 돌려서 빙글빙글 도는 느낌을 준다.

const SPIN_TIME = 3000 // 도는 시간 (3초)

// 당첨될 수 있는 분류. 판에는 서버가 주는 8가지(한식 · 중식 · 일식 ...)가 다 보이지만,
// 요리 100종이 거의 한식 · 국탕이라 나머지가 걸리면 추천이 텅 빈다. 그래서 바늘은 이 둘에만 멈춘다.
const WINNABLE = ['한식', '국탕']

// 조각 이름 위에 붙는 그림. 서버가 모르는 분류를 보내면 그림 없이 이름만 나온다
const EMOJI = {
  한식: '🍚',
  중식: '🥟',
  일식: '🍣',
  양식: '🍝',
  분식: '🍢',
  아시안: '🍜',
  샐러드: '🥗',
  국탕: '🍲',
}

// 판 테두리에 두르는 전구 개수
const BULBS = 16

export default function Roulette() {
  const [slices, setSlices] = useState([]) // 조각 목록 [{ label: '한식', color: '#f97316' }, ...]
  const [angle, setAngle] = useState(0) // 지금 돌아간 각도
  const [spinning, setSpinning] = useState(false) // 도는 중인지
  const [result, setResult] = useState(null) // 당첨된 조각
  const [foods, setFoods] = useState([]) // 당첨된 분류의 추천 음식 3개. 첫 번째가 오늘의 한 끼
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [popupOpen, setPopupOpen] = useState(false) // 당첨 창이 떠 있는지
  const [confetti, setConfetti] = useState(0) // 바뀔 때마다 폭죽이 한 번 터진다

  // 당첨 창은 Esc 로도 닫는다
  useEffect(() => {
    if (!popupOpen) {
      return
    }
    const onKey = (e) => {
      if (e.key === 'Escape') {
        setPopupOpen(false)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [popupOpen])

  // 룰렛 조각 목록을 서버에서 받아온다
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setSlices(data.roulette || [])
      } catch (err) {
        // 못 받아오면 빈 룰렛
      }
    }
    load()
  }, [])

  // 조각 하나의 각도. 조각이 6개면 60도
  const sliceDeg = 360 / slices.length

  // 배경색 만들기 → "conic-gradient(빨강 0deg 60deg, 파랑 60deg 120deg, ...)"
  const colorParts = slices.map((slice, i) => {
    const start = i * sliceDeg
    const end = (i + 1) * sliceDeg
    return `${slice.color} ${start}deg ${end}deg`
  })
  const wheelBackground = `conic-gradient(${colorParts.join(', ')})`

  // 돌리기 버튼
  const spin = () => {
    if (spinning || slices.length === 0) {
      return
    }

    // 1. 당첨 조각 번호를 뽑는다. WINNABLE 에 든 조각 중에서만 고른다
    //    (서버가 한식 · 국탕을 안 보내면 판 전체에서 고른다)
    const indexes = slices.map((slice, i) => i)
    let candidates = indexes.filter((i) => WINNABLE.includes(slices[i].label))
    if (candidates.length === 0) {
      candidates = indexes
    }
    const picked = candidates[Math.floor(Math.random() * candidates.length)]

    // 2. 당첨 조각의 가운데가 12시(바늘)에 오려면 얼마나 돌려야 하는지
    const pickedCenter = picked * sliceDeg + sliceDeg / 2
    const target = 360 - pickedCenter

    // 3. 지금 각도에서 5바퀴(360 * 5) 더 돌리고 target 만큼 더 간다
    const fullTurns = angle - (angle % 360) + 360 * 5
    setAngle(fullTurns + target)

    setSpinning(true)
    setResult(null)
    setFoods([])
    setError('')
    setPopupOpen(false)

    // 4. 다 돈 다음(3초 뒤)에 결과를 보여준다
    setTimeout(() => {
      setSpinning(false)
      setResult(slices[picked])
      setPopupOpen(true)
      setConfetti((count) => count + 1)

      loadFoods(slices[picked].label)
    }, SPIN_TIME)
  }

  // 당첨된 분류에서 음식 3개를 추천받는다
  const loadFoods = async (label) => {
    // 오늘의 한 끼를 못 고르면 '오늘의 게임 결과' 는 분류의 추천 목록으로 보낸다
    const categoryLink = `/recommend?category=${encodeURIComponent(label)}`

    setLoading(true)
    try {
      const data = await getRecommendations({ source: '메뉴 게임', limit: 3, category: label })
      const items = data.items || []
      setFoods(items)
      addLog('음식 룰렛', label, foodNames(items))

      // 메뉴 게임 화면의 '오늘의 게임 결과' 에 띄울 값. 오늘의 한 끼(첫 번째)의 레시피로 보낸다
      if (items.length > 0) {
        const top = items[0].food_name
        saveResult('roulette', '음식 룰렛', top, `/recipe/${top}`)
      } else {
        saveResult('roulette', '음식 룰렛', label, categoryLink)
      }
    } catch (err) {
      setError('추천 메뉴를 불러오지 못했어요.')
      saveResult('roulette', '음식 룰렛', label, categoryLink)
    } finally {
      setLoading(false)
    }
  }

  const reset = () => {
    setResult(null)
    setFoods([])
    setError('')
    setPopupOpen(false)
    setAngle(0)
  }

  // 당첨 창에서 '다시 돌리기'
  const spinAgain = () => {
    setPopupOpen(false)
    spin()
  }

  return (
    <section className="card roulette">
      <div className="section-head">
        <h2 className="section-title">음식 룰렛</h2>
        {/* 초기화. 그림은 public/images/game/icon-reset.png */}
        <button type="button" className="roulette__reset" onClick={reset} disabled={spinning}>
          <img src="/images/game/icon-reset.png" alt="" />
          <span className="roulette__reset-label">다시하기</span>
        </button>
      </div>

      <p className="muted roulette__desc">뭘 먹을지 고민될 땐 룰렛을 돌려보세요.</p>

      <div className={`roulette__stage${spinning ? ' roulette__stage--spinning' : ''}`}>
        {/* 테두리 전구. 판과 같이 돌지 않게 판 바깥에 둔다. 도는 동안 깜빡인다 */}
        {Array.from({ length: BULBS }, (_, i) => (
          <span key={i} className="roulette__bulb" style={{ transform: `rotate(${(360 / BULBS) * i}deg)` }} />
        ))}

        {/* 12시 방향 바늘 */}
        <span className="roulette__needle" />

        {/* 룰렛 판 */}
        <div
          className="roulette__wheel"
          style={{
            background: wheelBackground,
            transform: `rotate(${angle}deg)`,
            // 도는 중일 때만 3초 동안 부드럽게 움직인다
            transition: spinning ? `transform ${SPIN_TIME}ms ease-out` : 'none',
          }}
        >
          {/* 조각 이름. 각 조각의 가운데 각도만큼 돌려서 놓는다 */}
          {slices.map((slice, i) => (
            <span
              key={slice.label}
              className="roulette__label"
              style={{ transform: `translateX(-50%) rotate(${i * sliceDeg + sliceDeg / 2}deg)` }}
            >
              {EMOJI[slice.label] && <span className="roulette__label-emoji">{EMOJI[slice.label]}</span>}
              {slice.label}
            </span>
          ))}
        </div>

        <button type="button" className="roulette__spin" onClick={spin} disabled={spinning || slices.length === 0}>
          {spinning ? '…' : '돌리기'}
        </button>
      </div>

      {/* 창을 닫은 뒤 판 아래에 남는 결과 한 줄 */}
      {result && !popupOpen && (
        <div className="roulette__result">
          {foods.length > 0 ? (
            <>
              오늘의 한 끼는 <strong>{foods[0].food_name}</strong>
              <Link to={`/recipe/${foods[0].food_name}`} className="btn btn--sm">
                레시피 보기
              </Link>
            </>
          ) : (
            <>
              오늘은 <strong>{result.label}</strong> 어떠세요?
              <Link to={`/recommend?category=${encodeURIComponent(result.label)}`} className="btn btn--sm">
                추천 보기
              </Link>
            </>
          )}
          <button type="button" className="btn btn--ghost btn--sm" onClick={() => setPopupOpen(true)}>
            결과 다시 보기
          </button>
        </div>
      )}

      {/* 당첨 창. 바깥(어두운 곳)을 누르거나 Esc · × 로 닫는다 */}
      {result && popupOpen && (
        <div className="roulette-win" onClick={() => setPopupOpen(false)}>
          <div
            className="card roulette-win__box"
            role="dialog"
            aria-modal="true"
            aria-labelledby="roulette-win-title"
            onClick={(e) => e.stopPropagation()}
          >
            <button type="button" className="roulette-win__close" onClick={() => setPopupOpen(false)} aria-label="닫기">
              ×
            </button>

            <p className="roulette-win__kicker">
              {EMOJI[result.label] || '🎯'} {result.label} 당첨!
            </p>
            <h3 id="roulette-win-title" className="roulette-win__title">
              🎉 오늘의 한 끼!
            </h3>

            {loading && <Loading message={`${result.label} 중에서 고르는 중...`} />}

            {!loading && foods.length > 0 && (
              <>
                <div className="roulette-win__thumb">
                  <FoodThumb item={foods[0]} size="lg" />
                </div>
                <strong className="roulette-win__food">{foods[0].food_name}</strong>
                <p className="muted roulette-win__meta">
                  <span className="price">{formatPrice(foods[0].estimated_cost)}</span> ·{' '}
                  {formatMinutes(foods[0].cook_time_minutes)}
                </p>
              </>
            )}

            {!loading && foods.length === 0 && (
              <p className="roulette-win__empty">
                {error || `${result.label} 메뉴를 찾지 못했어요.`}
                <br />
                <Link to={`/recommend?category=${encodeURIComponent(result.label)}`}>{result.label} 추천 목록 보기 →</Link>
              </p>
            )}

            <div className="roulette-win__actions">
              {!loading && foods.length > 0 && (
                <Link to={`/recipe/${foods[0].food_name}`} className="btn">
                  레시피 보기
                </Link>
              )}
              <button type="button" className="btn btn--ghost game-btn--plain" onClick={spinAgain}>
                <span className="game-icon game-icon--restart" aria-hidden="true" />
                다시 돌리기
              </button>
            </div>

            {/* 나머지 추천 둘 */}
            {!loading && foods.length > 1 && (
              <div className="roulette-win__more">
                <p className="muted roulette-win__more-title">이런 메뉴도 있어요</p>
                <ul className="roulette-win__more-list">
                  {foods.slice(1).map((food) => (
                    <li key={food.food_name}>
                      <Link to={`/recipe/${food.food_name}`}>
                        <FoodThumb item={food} size="sm" />
                        <span>{food.food_name}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}

      <Confetti fire={confetti} />
    </section>
  )
}
