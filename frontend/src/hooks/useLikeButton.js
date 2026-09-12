import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useLikes } from '../contexts/LikesContext'

/**
 * 하트 버튼이 쓰는 훅.
 *
 * 찜은 로그인한 회원만 할 수 있다. 그래서 하트를 누를 때마다 세 화면
 * (FoodCard · RecipePage · WorldCupPage)이 똑같은 검사를 하게 된다.
 * 한 곳에 모아두지 않으면 나중에 하트가 하나 더 생겼을 때 빠뜨린다.
 *
 * 비로그인이면 로그인 화면으로 보낸다
 * ----------------------------------
 * 버튼을 비활성화만 하는 방법도 있다(게시글 좋아요가 그렇게 한다). 여기서는
 * 보내는 쪽을 골랐다 — 추천 목록을 훑다가 마음에 드는 걸 담으려는 동작이라,
 * "안 됩니다" 보다 "로그인하면 됩니다" 로 이어주는 편이 자연스럽다.
 *
 * 보던 화면으로 돌아온다
 * ---------------------
 * state.from 에 지금 주소를 넣어 보낸다. 로그인 후 대시보드로 떨어지면
 * 보던 추천 목록을 다시 찾아가야 하고, 찜하려던 음식도 놓친다.
 * ProtectedRoute 가 쓰는 것과 같은 방식이라 LoginPage 는 안 고쳐도 된다.
 *
 * 로그인 후 자동으로 찜해주지 않는다
 * ---------------------------------
 * 누르려던 의도를 이어주는 게 친절해 보이지만, 사용자가 로그인만 하러 간
 * 것인지 찜까지 원한 것인지 알 수 없다. 돌아와서 다시 누르면 된다.
 *
 * 이름을 인자가 아니라 호출할 때 받는다
 * ------------------------------------
 * 월드컵 화면은 우승자가 판이 끝나야 정해진다. 훅은 조건 없이 최상위에서
 * 불러야 하므로, 그때는 넘길 이름이 아직 없다. 그래서 훅에 묶지 않고
 * isLiked(name) · onLike(name) 으로 받는다.
 *
 * 쓰는 법
 * -------
 *   const { isLiked, onLike } = useLikeButton()
 *   <button onClick={() => onLike(food.food_name)} aria-pressed={isLiked(food.food_name)}>
 */
export function useLikeButton() {
  const { isAuthenticated } = useAuth()
  const { has, toggle } = useLikes()
  const navigate = useNavigate()
  const location = useLocation()

  function onLike(foodName) {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }

    toggle(foodName)
  }

  return {
    isLiked: has,
    onLike,
    // 화면이 안내 문구를 띄우고 싶을 때 쓴다
    needsLogin: !isAuthenticated,
  }
}
