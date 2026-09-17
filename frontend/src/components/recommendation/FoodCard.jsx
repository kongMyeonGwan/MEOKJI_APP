import { Link, useLocation, useNavigate } from 'react-router-dom'
import Icon from '../common/Icon'
import FoodThumb from '../common/FoodThumb'
import { useAuth } from '../../contexts/AuthContext'
import { useLikes } from '../../contexts/LikesContext'
import { formatMinutes, formatPrice } from '../../utils/format'
import '../../styles/components/recommendation/FoodCard.css'

// AI 가 추천한 음식 한 건을 카드로 보여준다.
// 추천 결과는 DB 에 저장하지 않고 사용자가 고른 것만 History 로 넘어간다.
//
// 하트를 카드 안에 뒀다. 레시피를 열어봐야 찜할 수 있으면
// 목록을 훑다가 마음에 든 걸 담는 게 매번 화면 이동이 된다.
export default function FoodCard({ food }) {
  const to = '/recipe/' + encodeURIComponent(food.food_name)

  const { isAuthenticated } = useAuth()
  const { has, toggle } = useLikes()
  const navigate = useNavigate()
  const location = useLocation()

  const liked = has(food.food_name)

  // 하트를 눌렀을 때.
  // 로그인 안 했으면 로그인 화면으로 보낸다.
  // 지금 주소를 같이 넘겨서 로그인하면 보던 화면으로 돌아오게 한다.
  function onLike() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }

    toggle(food.food_name)
  }

  let likeClass = 'food-card__like'
  if (liked) {
    likeClass = 'food-card__like is-on'
  }

  let likeLabel = food.food_name + ' 찜하기'
  let heartIcon = 'heart'
  if (liked) {
    likeLabel = food.food_name + ' 찜 해제'
    heartIcon = 'heart-fill'
  }

  let tags = food.tags
  if (!tags) {
    tags = []
  }

  return (
    <article className="food-card">
      <div className="food-card__media">
        <Link to={to} aria-label={food.food_name + ' 레시피 보기'}>
          <FoodThumb item={food} size="lg" />
        </Link>

        <button
          type="button"
          className={likeClass}
          onClick={onLike}
          aria-label={likeLabel}
        >
          <Icon name={heartIcon} size={19} />
        </button>
      </div>

      <div className="food-card__body">
        <header className="food-card__head">
          <h3 className="food-card__name">{food.food_name}</h3>
          <span className="price">{formatPrice(food.estimated_cost)}</span>
        </header>

        <p className="muted food-card__reason">{food.reason}</p>

        <div className="food-card__meta">
          <span className="tag tag--plain">
            🕐 {formatMinutes(food.cook_time_minutes)}
          </span>
          <span className="tag tag--plain">난이도 {food.difficulty}</span>
          {tags.map(function (tag) {
            return (
              <span key={tag} className="tag tag--plain food-card__hash">
                #{tag}
              </span>
            )
          })}
        </div>

        <Link to={to} className="btn btn--outline btn--block food-card__cta">
          레시피 보기
        </Link>
      </div>
    </article>
  )
}
