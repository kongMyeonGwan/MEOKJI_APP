import { formatPrice } from '../../utils/format'

// 예산 범위 슬라이더 (손잡이 2개).
// <input type="range"> 두 개를 겹쳐 놓고, 사이 구간만 색을 칠했다.
//   variant: 'inline'(첫 화면 카드) 이면 금액이 아래에, 아니면 위에 보인다.

// 한 끼 재료비 범위. 2만원을 한계로 둔 이유 — 카탈로그의 음식이
// 대부분 그 아래다. 10만원까지 열어두면 손잡이를 아주 조금 움직여도
// 금액이 몇천원씩 뛴다.
//
// export 하는 이유 — 쓰는 화면이 "지금 범위가 전체인지" 를 가려야 한다
// (전체면 예산 조건을 안 보낸다). 숫자를 화면에 다시 적으면 여기 값을
// 바꿀 때 어긋난다.
export const BUDGET_MIN = 0
export const BUDGET_MAX = 20000
// 1000원 단위로 움직인다. 100원 단위면 손잡이를 맞추기 어렵고,
// 재료비를 그만큼 정확히 고를 이유도 없다
const STEP = 1000

export default function BudgetRange({ min, max, onChange, variant }) {
  // 왼쪽 손잡이. 오른쪽 손잡이를 넘어가지 못하게 막는다.
  //
  // 안 막으면 min 이 max 보다 커져서, 서버가 "5천원 이상 3천원 이하" 를
  // 받고 아무것도 못 찾는다.
  //
  // max - STEP 까지만 허용하는 이유 — 둘이 같아지면 칠해진 구간이
  // 사라져서 손잡이가 어디 있는지 안 보인다.
  //
  // Number() 로 감싸는 이유 — <input> 이 주는 값은 늘 글자다.
  // 글자끼리 비교하면 "10000" < "9000" 이 된다(앞자리부터 견준다)
  const changeMin = (value) => {
    onChange(Math.min(Number(value), max - STEP), max)
  }

  // 오른쪽 손잡이. 왼쪽 손잡이보다 작아지지 못하게 막는다.
  const changeMax = (value) => {
    onChange(min, Math.max(Number(value), min + STEP))
  }

  // 금액을 막대 위의 위치(%)로 바꾼다. 5000원이면 25%
  //
  // px 이 아니라 % 로 두는 이유 — 막대 폭이 화면 크기에 따라 달라진다.
  // px 로 계산하면 창 크기를 바꿀 때 칠해진 구간이 손잡이와 어긋난다
  const toPercent = (value) => (value / BUDGET_MAX) * 100

  const inline = variant === 'inline'
  // 한계에 닿았으면 '+' 를 붙인다. "2만원" 으로만 적으면 그보다
  // 비싼 메뉴를 안 보여주는 것처럼 읽히는데, 실제로는 상한이 없다
  const valueText = `${formatPrice(min)} ~ ${formatPrice(max)}${max >= BUDGET_MAX ? '+' : ''}`

  return (
    <div className={inline ? 'budget budget--inline' : 'budget'}>
      {/* 금액과 양끝 눈금의 위아래 순서가 variant 에 따라 뒤바뀐다.
          첫 화면 카드(inline)는 금액을 아래에 둬야 카드 제목과 안 겹치고,
          예산 화면은 위에 둬야 손잡이를 움직이면서 바로 읽힌다.

          같은 것을 두 번 적은 모양이지만, 순서만 다른 두 덩어리라
          컴포넌트를 나누면 더 길어진다 */}
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

      {/* 막대는 네 겹이다
            budget__track : 회색 바탕
            budget__fill  : 고른 구간 색칠
            input 두 개   : 손잡이. 투명하게 만들어 위에 겹쳐 놓는다

          <input type="range"> 하나로는 손잡이가 하나뿐이라 범위를
          고를 수 없다. 두 개를 겹쳐 놓는 것이 흔히 쓰는 방법이다 */}
      <div className="budget__slider">
        <span className="budget__track" />
        {/* 고른 구간만 색칠.
            right 를 100 - 위치 로 적는 이유 — right 는 "오른쪽 끝에서
            얼마나 떨어졌는지" 다. 왼쪽 기준 위치를 100 에서 빼야 한다 */}
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
