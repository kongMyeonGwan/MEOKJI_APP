import { formatPrice } from '../../utils/format'
import '../../styles/components/recommendation/BudgetRange.css'

// 예산 범위 슬라이더. 손잡이가 양쪽에 하나씩 있다.
//
// <input type="range"> 두 개를 겹쳐 놓고 사이 구간만 색을 채웠다.
// 라이브러리를 안 쓴 이유는 이것 하나 때문에 설치하기 아깝고,
// 키보드로 조작하는 것도 기본 range 가 이미 된다.
//
// variant
//   'stacked'  제목이랑 값이 위에 (예산 전용 화면)
//   'inline'   양끝 금액이 위에, 고른 구간이 아래 (첫 화면 카드)

export const BUDGET_MIN = 0
export const BUDGET_MAX = 20000
const STEP = 1000

export default function BudgetRange({ min, max, onChange, variant }) {
  // 왼쪽(하한) 손잡이를 움직였을 때.
  // Math.min 으로 상한을 넘지 못하게 막는다. 안 막으면 구간이 뒤집힌다.
  // STEP 만큼 띄우는 건 둘이 완전히 겹쳐서 못 잡게 되는 걸 막으려는 것이다.
  function setMin(value) {
    let next = Number(value)
    if (next > max - STEP) {
      next = max - STEP
    }
    onChange(next, max)
  }

  // 오른쪽(상한) 손잡이를 움직였을 때. 하한보다 작아지지 않게 막는다.
  function setMax(value) {
    let next = Number(value)
    if (next < min + STEP) {
      next = min + STEP
    }
    onChange(min, next)
  }

  // 금액을 막대 위의 위치(%)로 바꾼다.
  // 0원이면 0%, 최대 금액이면 100%. 최대가 2만원일 때 5000원이면 25% 다.
  function pct(value) {
    return ((value - BUDGET_MIN) / (BUDGET_MAX - BUDGET_MIN)) * 100
  }

  const inline = variant === 'inline'

  let wrapClass = 'budget'
  if (inline) {
    wrapClass = 'budget budget--inline'
  }

  const ends = (
    <div className="budget__ends muted">
      <span>{formatPrice(BUDGET_MIN)}</span>
      <span>{formatPrice(BUDGET_MAX)}+</span>
    </div>
  )

  const value = (
    <>
      {formatPrice(min)} ~ {formatPrice(max)}
      {max >= BUDGET_MAX && '+'}
    </>
  )

  return (
    <div className={wrapClass}>
      {inline ? (
        ends
      ) : (
        <div className="budget__head">
          <span className="budget__label">예산 범위 설정</span>
          <strong className="budget__value">{value}</strong>
        </div>
      )}

      <div className="budget__slider">
        <span className="budget__track" />
        {/* 고른 구간만 주황색으로 채운다 */}
        <span
          className="budget__fill"
          style={{ left: pct(min) + '%', right: 100 - pct(max) + '%' }}
        />

        <input
          type="range"
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={STEP}
          value={min}
          onChange={(e) => setMin(e.target.value)}
          aria-label="최소 예산"
        />
        <input
          type="range"
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={STEP}
          value={max}
          onChange={(e) => setMax(e.target.value)}
          aria-label="최대 예산"
        />
      </div>

      {inline ? (
        <strong className="budget__value budget__value--center">{value}</strong>
      ) : (
        ends
      )}
    </div>
  )
}
