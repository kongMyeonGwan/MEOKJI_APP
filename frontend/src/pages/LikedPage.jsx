import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import FoodCard from '../components/recommendation/FoodCard'
import { useLikes } from '../contexts/LikesContext'
import { MOCK_RECOMMENDATIONS } from '../mocks'
import '../styles/pages/ListPage.css'

/**
 * 찜한 레시피 — 내가 하트를 누른 목록.
 *
 * 인기 레시피(/popular)와 헷갈리기 쉬운데 주인이 다르다.
 *   찜한 레시피  내가 담아둔 것. 남들의 좋아요 수와 무관하다
 *   인기 레시피  남들이 많이 누른 것
 *
 * 순서는 최근에 찜한 순이다(LikesContext가 앞에 넣는다).
 * 좋아요 수로 정렬하면 내가 방금 담은 게 목록 아래로 사라진다.
 *
 * 지금은 이름만 저장하고 화면에서 추천 목록과 이어 붙인다.
 * 서버에 찜 테이블이 생기면 이 join이 API 응답 하나로 바뀐다.
 */
export default function LikedPage() {
  const { names, count } = useLikes()

  const items = useMemo(
    () =>
      names
        .map((name) => MOCK_RECOMMENDATIONS.find((food) => food.food_name === name))
        // 추천 목록에서 사라진 메뉴가 찜에 남아 있을 수 있다.
        // 그 자리를 빈 카드로 두면 목록이 깨져 보인다
        .filter(Boolean),
    [names]
  )

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">찜한 레시피</h2>
        {count > 0 && <span className="muted section-more">{count}건</span>}
      </div>

      {items.length === 0 ? (
        <p className="empty">
          아직 찜한 레시피가 없어요.
          <br />
          카드 오른쪽 위의 하트를 누르면 여기에 모입니다.
          <br />
          <br />
          <Link to="/recommend" className="btn btn--outline btn--sm">
            추천 메뉴 보러 가기
          </Link>
        </p>
      ) : (
        <div className="list-page__cards">
          {items.map((food) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>
      )}
    </div>
  )
}
