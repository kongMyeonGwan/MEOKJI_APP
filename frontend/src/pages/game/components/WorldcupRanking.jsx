import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../../../components/common/FoodThumb'
import { getWorldcupRankings } from '../../../api/worldcup'
import Crown from './Crown'

// 월드컵 명예의 전당. 우승을 가장 많이 한 음식 TOP 5.
// 월드컵 라운드 고르는 화면 아래에 카드로 붙고, '명예의 전당' 버튼의 창(HallOfFameButton) 안에도 들어간다.
//
//   highlight : 강조할 음식 이름 (우승 화면에서 방금 우승한 메뉴). 순위에 있으면 줄을 띄운다
//   bare      : 창 안에 넣을 때. 카드 테두리와 제목을 빼고 목록만 그린다 (제목은 창이 그린다)
//
// 기록은 로그인한 회원이 월드컵을 끝까지 했을 때만 쌓인다 (WORLDCUP_WINS).
// 우승 횟수가 같으면 같은 등수이고, 결승 진출 횟수가 많은 쪽이 위에 놓인다.
export default function WorldcupRanking({ highlight, bare = false }) {
  const [data, setData] = useState(null) // { items, total }
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const load = async () => {
      try {
        setData(await getWorldcupRankings(5))
      } catch (err) {
        setFailed(true)
      }
    }
    load()
  }, [])

  // 받기 전에는 자리만 비워 둔다
  if (!data && !failed) {
    return null
  }

  return (
    <section className={bare ? 'wc-rank wc-rank--bare' : 'card wc-rank'}>
      <div className="wc-rank__head">
        {!bare && <h3 className="wc-rank__title">🏆 명예의 전당</h3>}
        {data && data.total > 0 && <span className="muted wc-rank__total">지금까지 {data.total.toLocaleString()}번의 월드컵</span>}
      </div>

      {failed && <p className="muted wc-rank__empty">순위를 불러오지 못했어요.</p>}

      {data && data.items.length === 0 && (
        <p className="muted wc-rank__empty">아직 우승 기록이 없어요. 첫 우승 메뉴를 만들어 보세요!</p>
      )}

      {data && data.items.length > 0 && (
        <ol className="wc-rank__list">
          {data.items.map((item) => (
            <li
              key={item.food_name}
              className={`wc-rank__item${item.rank === 1 ? ' wc-rank__item--top' : ''}${item.food_name === highlight ? ' wc-rank__item--mine' : ''}`}
            >
              <span className="wc-rank__no">
                {item.rank <= 3 && <Crown rank={item.rank} className="wc-rank__crown" />}
                {item.rank}위
              </span>
              <Link to={`/recipe/${item.food_name}`} className="wc-rank__food">
                <FoodThumb item={item} size="sm" />
                <strong>{item.food_name}</strong>
                {item.food_name === highlight && <span className="wc-rank__mine">오늘의 우승</span>}
              </Link>
              <span className="wc-rank__count">
                <strong>우승 {item.wins}회</strong>
                <span className="muted">결승 {item.finals}회</span>
              </span>
            </li>
          ))}
        </ol>
      )}

      <p className="muted wc-rank__note">로그인하고 끝까지 하면 우승 메뉴가 순위에 올라가요.</p>
    </section>
  )
}
