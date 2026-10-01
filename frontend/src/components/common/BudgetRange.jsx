import { formatPrice } from '../../utils/format'
import './BudgetRange.css'

// 예산 범위 슬라이더 (손잡이 2개).
// <input type="range"> 두 개를 겹쳐 놓고, 사이 구간만 색을 칠했다.
//   variant: 'inline'(첫 화면 카드) 이면 금액이 아래에, 아니면 위에 보인다.

export const BUDGET_MIN = 0
export const BUDGET_MAX = 20000
const STEP = 1000

export default function BudgetRange({ min, max, onChange, variant }) {
  // 왼쪽 손잡이. 오른쪽 손잡이를 넘어가지 못하게 막는다.
  const changeMin = (value) => {
    onChange(Math.min(Number(value), max - STEP), max)
  }

  // 오른쪽 손잡이. 왼쪽 손잡이보다 작아지지 못하게 막는다.
  const changeMax = (value) => {
    onChange(min, Math.max(Number(value), min + STEP))
  }

  // 금액을 막대 위의 위치(%)로 바꾼다. 5000원이면 25%
  const toPercent = (value) => (value / BUDGET_MAX) * 100

  const inline = variant === 'inline'
  const valueText = `${formatPrice(min)} ~ ${formatPrice(max)}${max >= BUDGET_MAX ? '+' : ''}`

  return (
    <div className={inline ? 'budget budget--inline' : 'budget'}>
      {inline ? (
        <div className="budget__ends muted">
          <span>{formatPrice(BUDGET_MIN)}</span>
          <span>{formatPrice(BUDGET_MAX)}+</span>
        </div>
      ) : (
        <div className="budget__head">
          <span className="budget__label">예산 범위 설정</span>
          <strong className="budget__value">{valueText}</strong>
        </div>
      )}

      <div className="budget__slider">
        <span className="budget__track" />
        {/* 고른 구간만 색칠 */}
        <span
          className="budget__fill"
          style={{ left: `${toPercent(min)}%`, right: `${100 - toPercent(max)}%` }}
        />
        <input
          type="range"
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={STEP}
          value={min}
          onChange={(e) => changeMin(e.target.value)}
        />
        <input
          type="range"
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={STEP}
          value={max}
          onChange={(e) => changeMax(e.target.value)}
        />
      </div>

      {inline ? (
        <strong className="budget__value budget__value--center">{valueText}</strong>
      ) : (
        <div className="budget__ends muted">
          <span>{formatPrice(BUDGET_MIN)}</span>
          <span>{formatPrice(BUDGET_MAX)}+</span>
        </div>
      )}
    </div>
  )
}
