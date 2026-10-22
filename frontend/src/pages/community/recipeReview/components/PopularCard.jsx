import { useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../../../../components/common/Icon'
import FoodThumb from '../../../../components/common/FoodThumb'
import { formatMinutes } from '../../../../utils/format'

// 인기 레시피 카드 한 장. 순위(rank)는 목록에서 몇 번째인지다.
//
// TODO(게시판 담당): 저장(북마크) 버튼은 지금 화면에서만 켜졌다 꺼진다.
//   찜 API 가 붙으면 FoodCard 처럼 toggleFavorite 를 부르고, 로그인 안 했으면
//   로그인 화면으로 보낸다.
export default function PopularCard({ item, rank }) {
  const [saved, setSaved] = useState(false)

  // 1 · 2 · 3 등만 색을 달리 준다 (금 · 은 · 동)
  const rankClass = () => {
    if (rank === 1) {
      return 'rank-card__rank is-gold'
    }
    if (rank === 2) {
      return 'rank-card__rank is-silver'
    }
    if (rank === 3) {
      return 'rank-card__rank is-bronze'
    }
    return 'rank-card__rank'
  }

  // 레시피가 아니라 '레시피 추천' 게시판의 후기 글로 간다
  const postUrl = `/community/${item.id}`

  return (
    <article className="rank-card">
      <div className="rank-card__media">
        <Link to={postUrl}>
          <FoodThumb item={item} size="lg" />
        </Link>

        <span className={rankClass()}>{rank}</span>
        <span className="rank-card__time">🕐 {formatMinutes(item.cook_time_minutes)}</span>
      </div>

      <div className="rank-card__body">
        <div className="rank-card__head">
          <h3 className="rank-card__name">
            {/* 레시피 이름이 없으면 음식 이름을 쓴다 */}
            <Link to={postUrl}>{item.recipe_name || item.food_name}</Link>
          </h3>

          <button
            type="button"
            className={saved ? 'rank-card__save is-on' : 'rank-card__save'}
            onClick={() => setSaved(!saved)}
            title={saved ? '저장 해제' : '저장'}
          >
            <Icon name={saved ? 'bookmark-fill' : 'bookmark'} size={19} />
          </button>
        </div>

        <p className="muted rank-card__summary">{item.summary}</p>

        <div className="rank-card__meta">
          <span className="rank-card__writer">
            <span className="rank-card__avatar">{item.nickname.slice(0, 1)}</span>
            {item.nickname}
          </span>

          <span className="rank-card__counts">
            <span className="rank-card__heart">♥ {item.like_count}</span>
            <span className="muted">💬 {item.comment_count}</span>
          </span>
        </div>

        <div className="rank-card__tags">
          {item.tags.map((tag) => (
            <span key={tag} className="tag rank-card__tag">
              #{tag}
            </span>
          ))}
        </div>
      </div>
    </article>
  )
}
