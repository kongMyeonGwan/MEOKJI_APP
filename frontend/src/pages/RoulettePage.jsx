import Roulette from '../components/recommendation/Roulette'
import '../styles/pages/ListPage.css'

/**
 * 음식 추천 룰렛 전용 화면.
 * 대시보드의 같은 카드를 넓게 펼친 것이다.
 *
 * 제목을 따로 두지 않는다. Roulette 카드가 이미 같은 제목을 달고 있어서
 * 화면에 "음식 추천 룰렛"이 두 번 보인다.
 */
export default function RoulettePage() {
  return (
    <div className="list-page list-page--narrow">
      <Roulette />
    </div>
  )
}
