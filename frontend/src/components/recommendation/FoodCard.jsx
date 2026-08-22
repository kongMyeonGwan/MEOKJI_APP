import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import { formatMinutes, formatPrice } from '../../utils/format'
import '../../styles/components/recommendation/FoodCard.css'

/**
 * AI가 추천한 음식 한 건.
 * 추천 결과는 DB에 저장하지 않는다. 사용자가 고른 것만 History로 넘어간다.
 */
export default function FoodCard({ food }) {
  const to = `/recipe/${encodeURIComponent(food.food_name)}`

  return (
    <article className="food-card">
      <Link to={to} aria-label={`${food.food_name} 레시피 보기`}>
        <FoodThumb item={food} size="lg" />
      </Link>

      <div className="food-card__body">
        <header className="food-card__head">
          <h3 className="food-card__name">{food.food_name}</h3>
          <span className="price">{formatPrice(food.estimated_cost)}</span>
        </header>

        <p className="muted food-card__reason">{food.reason}</p>

        <div className="food-card__meta">
          <span className="tag tag--plain">🕐 {formatMinutes(food.cook_time_minutes)}</span>
          <span className="tag tag--plain">난이도 {food.difficulty}</span>
          {food.tags?.map((tag) => (
            <span key={tag} className="tag tag--plain food-card__hash">
              #{tag}
            </span>
          ))}
        </div>

        <Link to={to} className="btn btn--outline btn--block food-card__cta">
          레시피 보기
        </Link>
      </div>
    </article>
  )
}
