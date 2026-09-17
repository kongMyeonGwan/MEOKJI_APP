import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import Loading from '../common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { useAiLog } from '../../contexts/AiLogContext'
import { formatPrice } from '../../utils/format'
import '../../styles/components/recommendation/BargainPick.css'

// "지금 싼 재료로" 카드. 요즘 값이 내린 재료로 만들 수 있는 메뉴를 보여준다.
//
// KAMIS 는 날마다 조사한 가격이라 "일주일 전보다 싼지" 를 알 수 있다.
// 그걸 뽑아서 AI 한테 넘기면 그 재료를 쓰는 메뉴를 골라준다.
//
// 위에는 싼 재료 칩을, 아래에는 그 재료로 만드는 메뉴를 보여준다.
// 재료를 먼저 보여줘야 "왜 이 메뉴지?" 가 안 된다.

// 한 번에 받아올 메뉴 개수
const PAGE_SIZE = 4

export default function BargainPick() {
  const { add } = useAiLog()

  const [items, setItems] = useState([])
  const [bargains, setBargains] = useState([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let alive = true

    getRecommendations({
      source: '시세 추천',
      useBargains: true,
      limit: PAGE_SIZE,
    })
      .then(function (data) {
        if (!alive) {
          return
        }

        let foods = data.items
        if (!foods) {
          foods = []
        }

        let cheap = data.bargains
        if (!cheap) {
          cheap = []
        }

        setItems(foods)
        setBargains(cheap)

        if (foods.length > 0) {
          const names = []
          for (let i = 0; i < foods.length; i++) {
            names.push(foods[i].food_name)
          }

          add({
            source: '시세 추천',
            condition: data.condition,
            foods: names,
          })
        }
      })
      .catch(function () {
        // 시세를 못 받아도 첫 화면의 다른 칸은 멀쩡해야 한다.
        // 에러 문구를 크게 띄우는 대신 카드를 통째로 숨긴다.
        if (alive) {
          setFailed(true)
        }
      })
      .finally(function () {
        if (alive) {
          setLoading(false)
        }
      })

    return function () {
      alive = false
    }
    // add 는 렌더할 때마다 새로 만들어져서 여기 넣으면 무한 호출이 된다
  }, [])

  if (failed) {
    return null
  }

  if (loading) {
    return (
      <section className="card bargain">
        <h2 className="section-title">지금 싼 재료로</h2>
        <Loading
          steps={['요즘 시세를 살펴보는 중...', 'AI가 메뉴를 고르는 중...']}
        />
      </section>
    )
  }

  // 값이 별로 안 움직인 날은 싼 재료가 없다. 그때는 카드를 안 띄운다.
  // "지금 싼 재료로" 라고 해놓고 아무것도 없으면 이상하다.
  if (bargains.length === 0 || items.length === 0) {
    return null
  }

  return (
    <section className="card bargain">
      <header className="bargain__head">
        <h2 className="section-title">지금 싼 재료로</h2>
        <p className="muted bargain__sub">일주일 전보다 값이 내렸어요</p>
      </header>

      <ul className="bargain__chips">
        {bargains.map(function (row) {
          const dropPercent = Math.abs(row.percent).toFixed(0)

          return (
            <li key={row.name} className="bargain__chip">
              <strong>{row.name}</strong>
              <span className="bargain__drop">{dropPercent}%↓</span>
              <span className="bargain__price">
                {formatPrice(row.price)}/{row.unit}
              </span>
            </li>
          )
        })}
      </ul>

      <ul className="bargain__foods">
        {items.map(function (food) {
          return (
            <li key={food.food_name}>
              <Link
                to={'/recipe/' + encodeURIComponent(food.food_name)}
                className="bargain__food"
              >
                <FoodThumb item={food} size="sm" />
                <span className="bargain__food-body">
                  <strong className="bargain__food-name">{food.food_name}</strong>
                  <span className="muted bargain__food-reason">{food.reason}</span>
                </span>
              </Link>
            </li>
          )
        })}
      </ul>

      <p className="muted bargain__note">
        한국농수산식품유통공사(KAMIS)가 조사한 전국 소매가격 기준입니다.
      </p>
    </section>
  )
}
