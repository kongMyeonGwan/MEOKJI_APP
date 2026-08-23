import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MOCK_ROULETTE } from '../../mocks'
import '../../styles/components/recommendation/Roulette.css'

/**
 * 음식 추천 룰렛.
 *
 * 뭘 먹을지 못 고를 때 종류부터 정해주는 장치다.
 * 결과를 고른 뒤 그 분류로 추천 화면(/recommend?category=한식)에 넘긴다.
 *
 * 회전은 CSS transform 하나로 처리한다.
 *   - 당첨 각도를 먼저 뽑고, 거기에 5바퀴(1800도)를 더해 돌린다.
 *     그래야 "한참 돌다가 멈추는" 느낌이 난다
 *   - 각도를 누적시키는 이유는 두 번째 회전이 0도로 되돌아갔다가
 *     다시 도는 역회전으로 보이지 않게 하기 위함이다
 */
const SLICE_COUNT = MOCK_ROULETTE.length
const SLICE_DEG = 360 / SLICE_COUNT
const SPIN_MS = 3200

/**
 * 회전 애니메이션을 끈 사용자인지.
 *
 * CSS(@media prefers-reduced-motion)에서 transition만 끄면 바퀴는 즉시 정답에
 * 멈추는데 결과 문구는 3.2초 뒤에 뜬다. 그 사이 버튼도 잠겨 있어 고장으로 보인다.
 * 기다리는 시간 자체를 JS에서도 없애야 앞뒤가 맞는다.
 */
function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

/** 원 위의 각도(도) -> SVG 좌표. 12시 방향이 0도, 시계방향이 양수 */
function pointOn(deg, radius, center = 50) {
  const rad = ((deg - 90) * Math.PI) / 180
  return [center + radius * Math.cos(rad), center + radius * Math.sin(rad)]
}

/** 한 조각의 부채꼴 path */
function slicePath(index) {
  const start = index * SLICE_DEG
  const end = start + SLICE_DEG
  const [x1, y1] = pointOn(start, 48)
  const [x2, y2] = pointOn(end, 48)
  // 조각이 180도를 넘지 않으므로 large-arc-flag 는 항상 0
  return `M50,50 L${x1},${y1} A48,48 0 0,1 ${x2},${y2} Z`
}

export default function Roulette() {
  const [angle, setAngle] = useState(0)
  const [spinning, setSpinning] = useState(false)
  const [result, setResult] = useState(null)
  const timer = useRef(null)
  const navigate = useNavigate()

  // 도는 도중에 다른 화면으로 넘어가면 타이머만 살아남아
  // 사라진 컴포넌트에 setState가 걸린다.
  useEffect(() => () => clearTimeout(timer.current), [])

  const spin = () => {
    if (spinning) return

    const picked = Math.floor(Math.random() * SLICE_COUNT)
    // 당첨 조각의 한가운데가 12시 바늘에 오도록 되돌릴 각도
    const target = 360 - (picked * SLICE_DEG + SLICE_DEG / 2)
    // 이미 돈 만큼을 버리지 않고 그 위에 5바퀴를 더 얹는다
    const next = angle - (angle % 360) + 360 * 5 + target

    setSpinning(true)
    setResult(null)
    setAngle(next)

    clearTimeout(timer.current)
    timer.current = setTimeout(
      () => {
        setSpinning(false)
        setResult(MOCK_ROULETTE[picked])
      },
      prefersReducedMotion() ? 0 : SPIN_MS
    )
  }

  return (
    <section className="card roulette">
      <div className="section-head">
        <h2 className="section-title">음식 추천 룰렛</h2>
        <button
          type="button"
          className="roulette__reset"
          onClick={() => {
            setResult(null)
            setAngle(0)
          }}
          disabled={spinning}
          aria-label="룰렛 초기화"
        >
          ↻
        </button>
      </div>

      <p className="muted roulette__desc">
        뭘 먹을지 고민될 땐 룰렛을 돌려보세요.
      </p>

      <div className="roulette__stage">
        {/* 12시 방향 바늘 */}
        <span className="roulette__needle" aria-hidden="true" />

        <svg
          className="roulette__wheel"
          viewBox="0 0 100 100"
          style={{
            transform: `rotate(${angle}deg)`,
            transition: spinning
              ? `transform ${SPIN_MS}ms cubic-bezier(0.16, 1, 0.3, 1)`
              : 'none',
          }}
          aria-hidden="true"
        >
          {MOCK_ROULETTE.map((slice, i) => (
            <path key={slice.label} d={slicePath(i)} fill={slice.color} />
          ))}

          {MOCK_ROULETTE.map((slice, i) => {
            // 글자는 조각 한가운데, 바깥쪽에 놓는다
            const mid = i * SLICE_DEG + SLICE_DEG / 2
            const [tx, ty] = pointOn(mid, 32)
            return (
              <text
                key={`t-${slice.label}`}
                x={tx}
                y={ty}
                className="roulette__slice-text"
                textAnchor="middle"
                dominantBaseline="middle"
                transform={`rotate(${mid} ${tx} ${ty})`}
              >
                {slice.label}
              </text>
            )
          })}
        </svg>

        <button
          type="button"
          className="roulette__spin"
          onClick={spin}
          disabled={spinning}
        >
          {spinning ? '…' : '돌리기'}
        </button>
      </div>

      {/* 결과는 회전이 끝난 뒤에만 뜬다. 도는 중에 보여주면 김이 샌다 */}
      {result && (
        <div className="roulette__result">
          오늘은 <strong>{result.label}</strong> 어떠세요?
          <button
            type="button"
            className="btn btn--sm"
            onClick={() =>
              navigate(`/recommend?category=${encodeURIComponent(result.label)}`)
            }
          >
            추천 보기
          </button>
        </div>
      )}
    </section>
  )
}
