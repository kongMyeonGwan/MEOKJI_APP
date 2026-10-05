import { Link } from 'react-router-dom'
import Icon from '../../../components/common/Icon'
import FoodThumb from '../../../components/common/FoodThumb'
import { formatMinutes, formatPrice } from '../../../utils/format'

// 찜한 레시피 카드 한 장 (격자 보기).
//
// item 은 서버가 /api/likes 의 items 로 주는 음식 정보다. 카탈로그에 없는 이름이면
// food_name 만 채워져서 오기 때문에, 나머지 칸은 있을 때만 그린다.
export default function LikedCard({ item, onUnlike }) {
  // 분류(한식)를 앞에 두고 태그를 뒤에 붙인다. 태그에 분류와 같은 말이 또 들어
  // 있어서(예: 한식) 그건 뺀다
  const tags = item.tags || []
  const extraTags = tags.filter((tag) => tag !== item.category)

  const recipeUrl = `/recipe/${item.food_name}`

  return (
    <article className="lcard">
      <div className="lcard__media">
        <Link to={recipeUrl}>
          <FoodThumb item={item} size="lg" />
        </Link>

        <button type="button" className="lcard__unlike" onClick={() => onUnlike(item.food_name)} title="찜 해제">
          <Icon name="heart-fill" size={19} />
        </button>
      </div>

      <div className="lcard__body">
        <div className="lcard__badges">
          {item.category && <span className="lcard__badge">{item.category}</span>}
          {extraTags.slice(0, 2).map((tag) => (
            <span key={tag} className="lcard__badge lcard__badge--plain">
              {tag}
            </span>
          ))}
        </div>

        <h3 className="lcard__name">
          <Link to={recipeUrl}>{item.food_name}</Link>
        </h3>

        {item.reason && <p className="muted lcard__desc">{item.reason}</p>}

        {/* 카탈로그에 없는 음식은 조리 시간이 0 으로 와서 이 줄을 통째로 숨긴다 */}
        {item.cook_time_minutes > 0 && (
          <p className="lcard__meta">
            <span>🕐 {formatMinutes(item.cook_time_minutes)}</span>
            <span>👨‍🍳 {item.difficulty}</span>
            <span className="lcard__price">💰 {formatPrice(item.estimated_cost)}</span>
          </p>
        )}

        <Link to={recipeUrl} className="btn btn--outline btn--block lcard__cta">
          레시피 보기 →
        </Link>
      </div>
    </article>
  )
}
