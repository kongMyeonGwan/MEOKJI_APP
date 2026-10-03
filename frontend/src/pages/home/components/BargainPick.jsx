import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../../../components/common/FoodThumb'
import Loading from '../../../components/common/Loading'
import { getRecommendations } from '../../../api/recommendations'
import { addLog, foodNames } from '../../../utils/aiLog'
import { formatPrice } from '../../../utils/format'

// "지금 싼 재료로" 카드 (첫 화면).
// 일주일 전보다 값이 내린 재료(bargains)와 그 재료로 만드는 메뉴(items)를 보여준다.
export default function BargainPick() {
  const [items, setItems] = useState([])
  const [bargains, setBargains] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getRecommendations({ source: '시세 추천', useBargains: true, limit: 4 })
        setItems(data.items || [])
        setBargains(data.bargains || [])
        addLog('시세 추천', data.condition, foodNames(data.items || []))
      } catch (err) {
        // 실패하면 카드를 안 보여준다 (아래에서 bargains 가 비어 있으면 숨김)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return (
      <section className="card bargain">
        <h2 className="section-title">지금 싼 재료로</h2>
        <Loading message="요즘 시세를 살펴보는 중..." />
      </section>
    )
  }

  // 싼 재료가 없는 날은 카드를 숨긴다
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
        {bargains.map((row) => (
          <li key={row.name} className="bargain__chip">
            <strong>{row.name}</strong>
            <span className="bargain__drop">{Math.abs(row.percent).toFixed(0)}%↓</span>
            <span className="bargain__price">
              {formatPrice(row.price)}/{row.unit}
            </span>
          </li>
        ))}
      </ul>

      <ul className="bargain__foods">
        {items.map((food) => (
          <li key={food.food_name}>
            <Link to={`/recipe/${food.food_name}`} className="bargain__food">
              <FoodThumb item={food} size="sm" />
              <span className="bargain__food-body">
                <strong className="bargain__food-name">{food.food_name}</strong>
                <span className="muted bargain__food-reason">{food.reason}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>

      <p className="muted bargain__note">한국농수산식품유통공사(KAMIS)가 조사한 전국 소매가격 기준입니다.</p>
    </section>
  )
}
