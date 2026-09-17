import { Link } from 'react-router-dom'
import FoodThumb from '../common/FoodThumb'
import { formatDate } from '../../utils/format'
import '../../styles/components/recommendation/LikedList.css'

// 좋아요 받은 레시피 목록. 첫 화면이랑 전체보기 화면에서 같이 쓴다.
export default function LikedList({ items }) {
  if (!items || items.length === 0) {
    return <p className="empty">이 기간에 좋아요를 받은 레시피가 없습니다.</p>
  }

  return (
    <ul className="liked">
      {items.map(function (item) {
        // 레시피 이름이 없으면 음식 이름을 쓴다
        let name = item.recipe_name
        if (!name) {
          name = item.food_name
        }

        return (
          <li key={item.id}>
            <Link
              to={'/recipe/' + encodeURIComponent(item.food_name)}
              className="liked__row"
            >
              <FoodThumb item={item} size="md" />

              <span className="liked__body">
                <span className="liked__name">{name}</span>
                <span className="liked__meta">
                  <span className="liked__heart">♥ {item.like_count}</span>
                  <span className="muted">{formatDate(item.liked_at)}</span>
                </span>
              </span>

              <span className="liked__arrow" aria-hidden="true">
                ›
              </span>
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
