import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import { formatDate } from '../../utils/format'
import '../../styles/components/recommendation/LikedList.css'

// 좋아요를 많이 받은 레시피 목록 (인기 레시피 화면)
export default function LikedList({ items }) {
  if (items.length === 0) {
    return <p className="empty">이 기간에 좋아요를 받은 레시피가 없습니다.</p>
  }

  return (
    <ul className="liked">
      {items.map((item) => (
        <li key={item.id}>
          <Link to={`/recipe/${item.food_name}`} className="liked__row">
            <FoodThumb item={item} size="md" />
            <span className="liked__body">
              {/* 레시피 이름이 없으면 음식 이름을 쓴다 */}
              <span className="liked__name">{item.recipe_name || item.food_name}</span>
              <span className="liked__meta">
                <span className="liked__heart">♥ {item.like_count}</span>
                <span className="muted">{formatDate(item.liked_at)}</span>
              </span>
            </span>
            <span className="liked__arrow">›</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}
