import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BudgetRange, { BUDGET_MAX, BUDGET_MIN } from './BudgetRange'
import { useCatalog } from '../../contexts/CatalogContext'
import '../../styles/components/recommendation/BudgetPicker.css'

/**
 * 예산 설정 카드 (대시보드).
 *
 * 자주 쓰는 상한을 칩으로 먼저 내놓고, 슬라이더는 그다음이다.
 * "만원 이하"처럼 이미 정해진 생각이 있는 사람이 대부분이라
 * 그 사람들이 손잡이를 두 번 끌게 만들 이유가 없다.
 *
 * 칩과 슬라이더는 같은 값을 본다. 칩을 누르면 슬라이더가 그 구간으로 옮겨가고,
 * 슬라이더를 직접 끌어 어느 칩과도 안 맞게 되면 칩 선택이 풀린다.
 * 둘이 따로 놀면 "1만원 이하"가 켜져 있는데 슬라이더는 2만원인 상태가 나온다.
 *
 * '상관없음'(max: null)만 성격이 다르다. 상한값이 아니라 예산 조건을 끄는
 * 선택지라서, 이때는 /budget 이 아니라 /recommend 로 보낸다.
 */
export default function BudgetPicker() {
  const [range, setRange] = useState({ min: BUDGET_MIN, max: BUDGET_MAX })
  const [preset, setPreset] = useState('UNDER_20K')
  const navigate = useNavigate()
  const { budgetPresets } = useCatalog()

  /** 금액 칩(5천원 이하 · 1만원 이하 ...)을 눌렀을 때. */
  function choosePreset(item) {
    setPreset(item.key)
    // '상관없음'은 슬라이더를 끝까지 펼친 모습으로 보여준다.
    // 조건이 없다는 걸 말로만 하지 않고 손잡이 위치로도 보여주는 편이 빠르다
    setRange({ min: BUDGET_MIN, max: item.max ?? BUDGET_MAX })
  }

  /** 슬라이더를 직접 끌었을 때. */
  function dragRange(min, max) {
    setRange({ min, max })
    // 직접 끌어 만든 구간이 우연히 칩과 같으면 그 칩을 켠 채로 둔다
    const matched = budgetPresets.find(
      (item) => item.max !== null && min === BUDGET_MIN && max === item.max
    )
    setPreset(matched?.key ?? null)
  }

  const noLimit = preset === 'ANY'

  /** 찾기 버튼을 눌렀을 때 예산을 들고 추천 화면으로 넘어간다. */
  function submit() {
    if (noLimit) {
      navigate('/recommend')
      return
    }
    // 상한이 최대치일 때도 max를 그대로 붙인다.
    // "최대치 = 제한 없음"이라는 해석은 받는 쪽(BudgetPage)에서만 한다.
    // 여기서 max를 생략하면 BudgetPage가 상한을 0원으로 읽어 전체 목록이 나온다.
    navigate(`/budget?min=${range.min}&max=${range.max}`)
  }

  return (
    <section className="card bpick">
      <h2 className="section-title">예산 설정</h2>

      <div className="bpick__row">
        <div className="chip-row bpick__presets">
          {budgetPresets.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`chip${preset === item.key ? ' is-active' : ''}`}
              onClick={() => choosePreset(item)}
              aria-pressed={preset === item.key}
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className="bpick__slider">
          <BudgetRange
            variant="inline"
            min={range.min}
            max={range.max}
            onChange={dragRange}
          />
        </div>

        <button type="button" className="btn bpick__submit" onClick={submit}>
          {noLimit ? '전체 메뉴 추천받기' : '이 예산으로 추천받기'}
        </button>
      </div>
    </section>
  )
}
