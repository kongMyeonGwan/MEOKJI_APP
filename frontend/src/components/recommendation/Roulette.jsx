import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCatalog } from '../../api/catalog'
import '../../styles/components/recommendation/Roulette.css'

// 음식 추천 룰렛. 돌려서 나온 종류(한식, 중식...)로 추천 화면에 넘어간다.
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

export default function Roulette() {
  const [slices, setSlices] = useState([]) // 조각 목록 [{ label: '한식', color: '#f97316' }, ...]
  const [angle, setAngle] = useState(0) // 지금 돌아간 각도
  const [spinning, setSpinning] = useState(false) // 도는 중인지
  const [result, setResult] = useState(null) // 당첨된 조각

  const navigate = useNavigate()

  // 룰렛 조각 목록을 서버에서 받아온다
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setSlices(data.roulette)
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

    // 1. 당첨 조각 번호를 뽑는다 (0 ~ 조각수-1)
    const picked = Math.floor(Math.random() * slices.length)

    // 2. 당첨 조각의 가운데가 12시(바늘)에 오려면 얼마나 돌려야 하는지
    const pickedCenter = picked * sliceDeg + sliceDeg / 2
    const target = 360 - pickedCenter

    // 3. 지금 각도에서 5바퀴(360 * 5) 더 돌리고 target 만큼 더 간다
    const fullTurns = angle - (angle % 360) + 360 * 5
    setAngle(fullTurns + target)

    setSpinning(true)
    setResult(null)

    // 4. 다 돈 다음(3초 뒤)에 결과를 보여준다
    setTimeout(() => {
      setSpinning(false)
      setResult(slices[picked])
    }, SPIN_TIME)
  }

  const reset = () => {
    setResult(null)
    setAngle(0)
  }

  return (
    <section className="card roulette">
      <div className="section-head">
        <h2 className="section-title">음식 추천 룰렛</h2>
        <button type="button" className="roulette__reset" onClick={reset} disabled={spinning}>
          ↻
        </button>
      </div>

      <p className="muted roulette__desc">뭘 먹을지 고민될 땐 룰렛을 돌려보세요.</p>

      <div className="roulette__stage">
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
              {slice.label}
            </span>
          ))}
        </div>

        <button type="button" className="roulette__spin" onClick={spin} disabled={spinning || slices.length === 0}>
          {spinning ? '…' : '돌리기'}
        </button>
      </div>

      {result && (
        <div className="roulette__result">
          오늘은 <strong>{result.label}</strong> 어떠세요?
          <button type="button" className="btn btn--sm" onClick={() => navigate(`/recommend?category=${result.label}`)}>
            추천 보기
          </button>
        </div>
      )}
    </section>
  )
}
