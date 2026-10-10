import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from './Icon'
import FoodThumb from './FoodThumb'
import { getLikes, toggleLike } from '../../api/likes'
import { getToken } from '../../utils/token'
import { formatMinutes, formatPrice } from '../../utils/format'

// AI 가 추천한 음식 카드 한 장. 오른쪽 위 하트로 찜할 수 있다.
//
// 추천 · 검색 · 예산 · 찜 목록이 같이 쓴다. 카드 모양을 화면마다
// 적으면 한 곳만 고쳐도 다른 화면과 어긋난다.
//
// 찜 상태를 카드가 스스로 들고 있다
//
// 카드마다 getLikes() 를 부르므로, 카드가 다섯 장이면 같은 요청이
// 다섯 번 나간다. 부모가 한 번 받아 내려주는 쪽이 가볍지만, 그러면
// 카드를 쓰는 화면 네 곳이 전부 그 처리를 해야 한다.
//
// 지금은 카드 수가 적어서 그대로 두었다. 목록이 길어지면 부모가
// 받아 내려주는 쪽으로 바꿀 자리다.
export default function FoodCard({ food }) {
  const [liked, setLiked] = useState(false)
  const navigate = useNavigate()

  // 로그인했으면 이 음식을 찜해뒀는지 확인한다
  //
  // user 를 props 로 받지 않고 getToken() 으로 직접 본다. 카드를
  // 쓰는 화면 네 곳이 전부 user 를 내려주게 하지 않으려는 것이다 -
  // 대신 "로그인 상태가 바뀌면 카드가 다시 그려진다" 는 보장이 없다
  useEffect(() => {
    if (!getToken()) {
      return
    }

    const load = async () => {
      try {
        const data = await getLikes()
        // names 는 찜한 음식 이름 목록이다. 그 안에 이 음식이
        // 있는지만 보면 된다
        setLiked(data.names.includes(food.food_name))
      } catch (err) {
        // 몰라도 하트가 빈 채로 보일 뿐이다
        //
        // 카드를 아예 못 그리게 하면 추천 목록이 통째로 빈다.
        // 하트 하나보다 목록이 보이는 쪽이 중요하다
      }
    }
    load()
    // 음식이 바뀌면 다시 본다. 월드컵처럼 같은 자리에 다른 음식이
    // 들어오는 화면이 있어서, [] 로 두면 앞 음식의 하트가 남는다
  }, [food.food_name])

  // 하트를 눌렀을 때. 로그인 안 했으면 로그인 화면으로 보낸다.
  //
  // 하트를 아예 숨기지 않는 이유 — 숨기면 그런 기능이 있는 줄
  // 모른다. 눌러보고 로그인 화면으로 가면 "로그인하면 되는구나" 가 된다
  const handleLike = async () => {
    if (!getToken()) {
      navigate('/login')
      return
    }

    try {
      const result = await toggleLike(food.food_name)
      // 서버가 알려준 상태를 그대로 쓴다. !liked 로 뒤집으면
      // 요청이 실패했을 때 하트만 바뀐 채로 남는다
      setLiked(result.liked)
    } catch (err) {
      // alert 를 쓰는 자리다. 카드 안에 에러 문구를 띄울 공간이 없다
      alert('찜하지 못했습니다.')
    }
  }

  // 같은 주소를 두 군데(사진 · 버튼)에서 쓴다. 변수로 빼두면
  // 한쪽만 고치는 일이 없다
  const recipeUrl = `/recipe/${food.food_name}`
  // 태그가 없는 음식이 있다. || [] 가 없으면 아래 .map 에서 터진다
  const tags = food.tags || []

  return (
    <article className="food-card">
      <div className="food-card__media">
        <Link to={recipeUrl}>
          <FoodThumb item={food} size="lg" />
        </Link>

        {/* 하트. 사진 안에 겹쳐 놓는다(CSS).
            빈 하트와 찬 하트를 다른 그림으로 바꿔서, 색만 바꾸는
            것보다 눌렸다는 것이 분명히 보인다 */}
        <button type="button" className={liked ? 'food-card__like is-on' : 'food-card__like'} onClick={handleLike}>
          <Icon name={liked ? 'heart-fill' : 'heart'} size={19} />
        </button>
      </div>

      <div className="food-card__body">
        <header className="food-card__head">
          <h3 className="food-card__name">{food.food_name}</h3>
          <span className="price">{formatPrice(food.estimated_cost)}</span>
        </header>

        {/* 왜 이 메뉴가 나왔는지. 서버가 점수를 매기면서 적어 보낸다 -
            점수(숫자)를 그대로 띄우면 그게 무슨 뜻인지 알 수 없다 */}
        <p className="muted food-card__reason">{food.reason}</p>

        <div className="food-card__meta">
          <span className="tag tag--plain">🕐 {formatMinutes(food.cook_time_minutes)}</span>
          <span className="tag tag--plain">난이도 {food.difficulty}</span>
          {/* 태그를 전부 그린다. 개수를 제한하지 않은 이유 —
              카탈로그의 태그가 음식마다 두세 개뿐이다 */}
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
