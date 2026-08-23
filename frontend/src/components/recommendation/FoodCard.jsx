import { Link } from 'react-router-dom'
import Icon from '../common/Icon'
import FoodThumb from '../common/FoodThumb'
import { useLikes } from '../../contexts/LikesContext'
import { formatMinutes, formatPrice } from '../../utils/format'
import '../../styles/components/recommendation/FoodCard.css'

/**
 * AI가 추천한 음식 한 건.
 * 추천 결과는 DB에 저장하지 않는다. 사용자가 고른 것만 History로 넘어간다.
 *
 * 하트는 카드 안에 둔다. 레시피를 열어봐야 찜할 수 있으면
 * 목록을 훑다가 마음에 든 걸 담아두는 동작이 매번 화면 이동이 된다.
 */
export default function FoodCard({ food }) {
  const to = `/recipe/${encodeURIComponent(food.food_name)}`
  const { has, toggle } = useLikes()
  const liked = has(food.food_name)

  return (
    <article className="food-card">
      <div className="food-card__media">
        <Link to={to} aria-label={`${food.food_name} 레시피 보기`}>
          <FoodThumb item={food} size="lg" />
        </Link>

        <button
          type="button"
          className={`food-card__like${liked ? ' is-on' : ''}`}
          onClick={() => toggle(food.food_name)}
          aria-pressed={liked}
          aria-label={liked ? `${food.food_name} 찜 해제` : `${food.food_name} 찜하기`}
        >
          <Icon name={liked ? 'heart-fill' : 'heart'} size={19} />
        </button>
      </div>

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
