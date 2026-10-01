import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from './Icon'
import FoodThumb from './FoodThumb'
import { getLikes, toggleLike } from '../../api/likes'
import { getToken } from '../../utils/token'
import { formatMinutes, formatPrice } from '../../utils/format'
import './FoodCard.css'

// AI 가 추천한 음식 카드 한 장. 오른쪽 위 하트로 찜할 수 있다.
export default function FoodCard({ food }) {
  const [liked, setLiked] = useState(false)
  const navigate = useNavigate()

  // 로그인했으면 이 음식을 찜해뒀는지 확인한다
  useEffect(() => {
    if (!getToken()) {
      return
    }

    const load = async () => {
      try {
        const data = await getLikes()
        setLiked(data.names.includes(food.food_name))
      } catch (err) {
        // 몰라도 하트가 빈 채로 보일 뿐이다
      }
    }
    load()
  }, [food.food_name])

  // 하트를 눌렀을 때. 로그인 안 했으면 로그인 화면으로 보낸다.
  const handleLike = async () => {
    if (!getToken()) {
      navigate('/login')
      return
    }

    try {
      const result = await toggleLike(food.food_name)
      setLiked(result.liked)
    } catch (err) {
      alert('찜하지 못했습니다.')
    }
  }

  const recipeUrl = `/recipe/${food.food_name}`
  const tags = food.tags || []

  return (
    <article className="food-card">
      <div className="food-card__media">
        <Link to={recipeUrl}>
          <FoodThumb item={food} size="lg" />
        </Link>

        <button type="button" className={liked ? 'food-card__like is-on' : 'food-card__like'} onClick={handleLike}>
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
          {tags.map((tag) => (
            <span key={tag} className="tag tag--plain food-card__hash">
              #{tag}
            </span>
          ))}
        </div>

        <Link to={recipeUrl} className="btn btn--outline btn--block food-card__cta">
          레시피 보기
        </Link>
      </div>
    </article>
  )
}
