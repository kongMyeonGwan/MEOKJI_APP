import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BudgetRange, { BUDGET_MAX, BUDGET_MIN } from './BudgetRange'
import { useCatalog } from '../../contexts/CatalogContext'
import '../../styles/components/recommendation/BudgetPicker.css'

// 첫 화면의 예산 설정 카드.
//
// 자주 쓰는 금액을 칩으로 먼저 보여주고 슬라이더는 그 다음이다.
// "만원 이하" 처럼 이미 생각이 정해진 사람이 많아서
// 굳이 손잡이를 두 번 끌게 할 이유가 없다.
//
// 칩이랑 슬라이더는 같은 값을 본다. 칩을 누르면 슬라이더가 따라 움직이고,
// 슬라이더를 끌어서 어느 칩이랑도 안 맞으면 칩 선택이 풀린다.
//
// '상관없음' 만 성격이 다르다. 금액이 아니라 예산 조건을 끄는 거라서
// 이때는 /budget 이 아니라 /recommend 로 보낸다.
export default function BudgetPicker() {
  const [min, setMin] = useState(BUDGET_MIN)
  const [max, setMax] = useState(BUDGET_MAX)
  const [preset, setPreset] = useState('UNDER_20K')

  const navigate = useNavigate()
  const { budgetPresets } = useCatalog()

  // 금액 칩(5천원 이하, 1만원 이하 ...)을 눌렀을 때
  function choosePreset(item) {
    setPreset(item.key)
    setMin(BUDGET_MIN)

    // '상관없음' 은 슬라이더를 끝까지 펼쳐서 보여준다.
    // 조건이 없다는 걸 손잡이 위치로도 보여주는 게 빠르다.
    if (item.max === null) {
      setMax(BUDGET_MAX)
    } else {
      setMax(item.max)
    }
  }

  // 슬라이더를 직접 끌었을 때
  function dragRange(nextMin, nextMax) {
    setMin(nextMin)
    setMax(nextMax)

    // 끌어서 만든 구간이 우연히 칩이랑 같으면 그 칩을 켠 채로 둔다
    let matchedKey = null
    for (let i = 0; i < budgetPresets.length; i++) {
      const item = budgetPresets[i]
      if (item.max !== null && nextMin === BUDGET_MIN && nextMax === item.max) {
        matchedKey = item.key
        break
      }
    }

    setPreset(matchedKey)
  }

  const noLimit = preset === 'ANY'

  // 찾기 버튼을 누르면 예산을 들고 추천 화면으로 넘어간다
  function submit() {
    if (noLimit) {
      navigate('/recommend')
      return
    }

    // 상한이 최대치여도 max 를 그대로 붙인다.
    // "최대치 = 제한 없음" 이라는 해석은 받는 쪽(BudgetPage)에서만 한다.
    // 여기서 빼면 BudgetPage 가 상한을 0원으로 읽어서 전체 목록이 나온다.
    navigate('/budget?min=' + min + '&max=' + max)
  }

  let submitLabel = '이 예산으로 추천받기'
  if (noLimit) {
    submitLabel = '전체 메뉴 추천받기'
  }

  return (
    <section className="card bpick">
      <h2 className="section-title">예산 설정</h2>

      <div className="bpick__row">
        <div className="chip-row bpick__presets">
          {budgetPresets.map(function (item) {
            let chipClass = 'chip'
            if (preset === item.key) {
              chipClass = 'chip is-active'
            }

            return (
              <button
                key={item.key}
                type="button"
                className={chipClass}
                onClick={() => choosePreset(item)}
              >
                {item.label}
              </button>
            )
          })}
        </div>

        <div className="bpick__slider">
          <BudgetRange
            variant="inline"
            min={min}
            max={max}
            onChange={dragRange}
          />
        </div>

        <button type="button" className="btn bpick__submit" onClick={submit}>
          {submitLabel}
        </button>
      </div>
    </section>
  )
}
