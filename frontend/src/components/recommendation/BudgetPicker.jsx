import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BudgetRange, { BUDGET_MAX, BUDGET_MIN } from './BudgetRange'
import '../../styles/components/recommendation/BudgetPicker.css'

// 첫 화면의 예산 설정 카드. 금액 칩을 누르거나 슬라이더를 끌어서 예산을 정한다.
//   budgetPresets : [{ key: 'UNDER_10K', label: '1만원 이하', max: 10000 }, ...]
//                   '상관없음' 칩은 key 가 'ANY' 이고 max 가 null 이다.
export default function BudgetPicker({ budgetPresets }) {
  const [min, setMin] = useState(BUDGET_MIN)
  const [max, setMax] = useState(BUDGET_MAX)
  const [preset, setPreset] = useState('UNDER_20K') // 지금 켜진 칩의 key

  const navigate = useNavigate()

  // 금액 칩을 눌렀을 때 슬라이더도 같이 맞춘다
  const choosePreset = (item) => {
    setPreset(item.key)
    setMin(BUDGET_MIN)
    setMax(item.max === null ? BUDGET_MAX : item.max)
  }

  // 슬라이더를 직접 끌었을 때. 어느 칩과도 안 맞으면 칩 선택을 끈다.
  const dragRange = (nextMin, nextMax) => {
    setMin(nextMin)
    setMax(nextMax)

    const matched = budgetPresets.find((item) => nextMin === BUDGET_MIN && item.max === nextMax)
    setPreset(matched ? matched.key : null)
  }

  const handleSubmit = () => {
    // '상관없음' 이면 예산 조건 없이 추천 화면으로
    if (preset === 'ANY') {
      navigate('/recommend')
      return
    }
    navigate(`/budget?min=${min}&max=${max}`)
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
              className={preset === item.key ? 'chip is-active' : 'chip'}
              onClick={() => choosePreset(item)}
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className="bpick__slider">
          <BudgetRange variant="inline" min={min} max={max} onChange={dragRange} />
        </div>

        <button type="button" className="btn bpick__submit" onClick={handleSubmit}>
          {preset === 'ANY' ? '전체 메뉴 추천받기' : '이 예산으로 추천받기'}
        </button>
      </div>
    </section>
  )
}
