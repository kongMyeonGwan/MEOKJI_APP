import { formatPrice } from '../../utils/format'
import '../../styles/components/recommendation/BudgetRange.css'

/**
 * 예산 범위 슬라이더 (양쪽 손잡이).
 *
 * <input type="range"> 두 개를 겹쳐 놓고, 사이 구간만 색을 채운다.
 * 라이브러리를 쓰지 않는 이유는 이것 하나 때문에 의존성을 늘리기 아깝고,
 * 키보드 조작·접근성은 네이티브 range가 이미 갖추고 있기 때문이다.
 *
 * 겹쳐진 두 input 중 위에 있는 쪽이 클릭을 다 먹으므로,
 * 트랙은 pointer-events: none 으로 두고 손잡이만 다시 켠다(CSS 참고).
 *
 * variant
 *   'stacked'  제목 + 값이 위, 슬라이더, 양끝 금액 (전용 화면용)
 *   'inline'   양끝 금액이 위, 슬라이더, 고른 구간이 아래 한 줄
 *              (대시보드처럼 카드 제목이 이미 "예산 설정"인 자리)
 */
export const BUDGET_MIN = 0
export const BUDGET_MAX = 20000
const STEP = 1000

export default function BudgetRange({ min, max, onChange, variant = 'stacked' }) {
  // 두 손잡이가 서로를 넘어가지 못하게 막는다. 넘어가면 구간이 뒤집힌다.
  const setMin = (v) => onChange(Math.min(Number(v), max - STEP), max)
  const setMax = (v) => onChange(min, Math.max(Number(v), min + STEP))

  const pct = (v) => ((v - BUDGET_MIN) / (BUDGET_MAX - BUDGET_MIN)) * 100

  const inline = variant === 'inline'

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
    <div className={`budget${inline ? ' budget--inline' : ''}`}>
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
        {/* 선택된 구간만 브랜드색으로 채운다 */}
        <span
          className="budget__fill"
          style={{ left: `${pct(min)}%`, right: `${100 - pct(max)}%` }}
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

      {inline ? <strong className="budget__value budget__value--center">{value}</strong> : ends}
    </div>
  )
}
