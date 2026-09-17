import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCatalog } from '../../contexts/CatalogContext'
import '../../styles/components/recommendation/Roulette.css'

// 음식 추천 룰렛.
// 뭘 먹을지 못 고를 때 종류부터 정해준다.
// 돌려서 나온 분류로 /recommend?category=한식 로 넘어간다.
//
// 돌리는 건 CSS transform 하나로 한다.
//   - 당첨 각도를 먼저 뽑고 거기에 5바퀴를 더해서 돌린다.
//     그래야 한참 돌다가 멈추는 느낌이 난다.
//   - 각도를 계속 더하는 이유는 두 번째로 돌릴 때 0도로 돌아갔다가
//     다시 도는 역회전처럼 보이지 않게 하려는 것이다.
//
// 조각 목록은 서버에서 온다. 개수가 고정이 아니라서
// 조각 각도(360 / 개수)를 그릴 때마다 계산한다.

const SPIN_MS = 3200

// 화면 움직임을 꺼둔 사용자인지 확인한다.
// CSS 에서 애니메이션만 끄면 바퀴는 바로 멈추는데 결과 문구는 3.2초 뒤에 떠서
// 그 사이에 버튼이 잠긴 채로 있다. 기다리는 시간도 같이 없애야 말이 맞는다.
function prefersReducedMotion() {
  if (!window.matchMedia) {
    // 아주 옛날 브라우저에는 이 함수가 없다
    return false
  }
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

// 원 위의 각도를 SVG 좌표로 바꾼다. 12시 방향이 0도, 시계방향이 +
function pointOn(deg, radius) {
  const center = 50
  const rad = ((deg - 90) * Math.PI) / 180
  return [center + radius * Math.cos(rad), center + radius * Math.sin(rad)]
}

// 조각 하나의 부채꼴 모양을 만든다
function slicePath(index, sliceDeg) {
  const start = index * sliceDeg
  const end = start + sliceDeg

  const startPoint = pointOn(start, 48)
  const endPoint = pointOn(end, 48)

  // 조각이 180도를 넘으면 SVG 한테 "먼 쪽 호" 라고 알려줘야 한다.
  // 조각이 두 개뿐일 때(각 180도) 필요하다.
  let largeArc = 0
  if (sliceDeg > 180) {
    largeArc = 1
  }

  return (
    'M50,50 L' + startPoint[0] + ',' + startPoint[1] +
    ' A48,48 0 ' + largeArc + ',1 ' + endPoint[0] + ',' + endPoint[1] + ' Z'
  )
}

export default function Roulette() {
  const { roulette } = useCatalog()
  const [angle, setAngle] = useState(0)
  const [spinning, setSpinning] = useState(false)
  const [result, setResult] = useState(null)
  const timer = useRef(null)
  const navigate = useNavigate()

  // 도는 중에 다른 화면으로 넘어가면 타이머만 살아남는다.
  // 그러면 사라진 컴포넌트에 setState 가 걸린다.
  useEffect(() => {
    return function () {
      clearTimeout(timer.current)
    }
  }, [])

  const sliceCount = roulette.length

  let sliceDeg = 360
  if (sliceCount > 0) {
    sliceDeg = 360 / sliceCount
  }

  // 돌리기 버튼을 눌렀을 때
  function spin() {
    if (spinning) {
      // 도는 중에 또 누르면 무시한다
      return
    }
    if (sliceCount === 0) {
      // 아직 목록을 못 받았다
      return
    }

    const picked = Math.floor(Math.random() * sliceCount)
    // 당첨 조각의 한가운데가 12시 바늘에 오도록 되돌릴 각도
    const target = 360 - (picked * sliceDeg + sliceDeg / 2)
    // 이미 돈 만큼을 버리지 않고 그 위에 5바퀴를 더 얹는다
    const next = angle - (angle % 360) + 360 * 5 + target

    setSpinning(true)
    setResult(null)
    setAngle(next)

    let waitMs = SPIN_MS
    if (prefersReducedMotion()) {
      waitMs = 0
    }

    clearTimeout(timer.current)
    timer.current = setTimeout(function () {
      setSpinning(false)
      setResult(roulette[picked])
    }, waitMs)
  }

  // 도는 중일 때만 애니메이션을 켠다
  let wheelTransition = 'none'
  if (spinning) {
    wheelTransition = 'transform ' + SPIN_MS + 'ms cubic-bezier(0.16, 1, 0.3, 1)'
  }

  let spinLabel = '돌리기'
  if (spinning) {
    spinLabel = '…'
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
          style={{ transform: 'rotate(' + angle + 'deg)', transition: wheelTransition }}
          aria-hidden="true"
        >
          {roulette.map(function (slice, i) {
            return (
              <path
                key={slice.label}
                d={slicePath(i, sliceDeg)}
                fill={slice.color}
              />
            )
          })}

          {roulette.map(function (slice, i) {
            // 글자는 조각 한가운데, 바깥쪽에 놓는다
            const mid = i * sliceDeg + sliceDeg / 2
            const point = pointOn(mid, 32)
            const tx = point[0]
            const ty = point[1]

            return (
              <text
                key={'t-' + slice.label}
                x={tx}
                y={ty}
                className="roulette__slice-text"
                textAnchor="middle"
                dominantBaseline="middle"
                transform={'rotate(' + mid + ' ' + tx + ' ' + ty + ')'}
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
          disabled={spinning || sliceCount === 0}
        >
          {spinLabel}
        </button>
      </div>

      {/* 결과는 다 돌고 나서만 보여준다. 도는 중에 보여주면 김이 샌다 */}
      {result && (
        <div className="roulette__result">
          오늘은 <strong>{result.label}</strong> 어떠세요?
          <button
            type="button"
            className="btn btn--sm"
            onClick={() =>
              navigate('/recommend?category=' + encodeURIComponent(result.label))
            }
          >
            추천 보기
          </button>
        </div>
      )}
    </section>
  )
}
