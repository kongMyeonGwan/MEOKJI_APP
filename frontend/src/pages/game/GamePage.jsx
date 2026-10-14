import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../../components/common/FoodThumb'
import { getCatalog } from '../../api/catalog'
import { getHistories } from '../../api/histories'
import { getTodayResult } from '../../utils/gameResult'
import { getToken } from '../../utils/token'
import { formatDate } from '../../utils/format'

// 메뉴 게임 목록 (월드컵 / 밸런스 / 룰렛).
//   위   : 배너
//   가운데: 게임 카드 셋
//   아래 : [오늘의 게임 결과][최근 선택한 메뉴]
//
// 게임 목록은 서버(/api/catalog)에서 온다.

// 게임마다 고정인 꾸밈. 서버가 주는 key 로 찾는다.
// 색은 로그인 화면의 게임 배너(GamePromo)와 같은 값을 쓴다 - 같은 게임이
// 화면마다 다른 색이면 어색하다.
const TONES = {
  worldcup: 'orange',
  balance: 'purple',
  roulette: 'yellow',
}

// 게임 이름. 서버(catalog.json)가 주는 title 보다 이 값을 먼저 쓴다.
// 각 게임 화면의 제목과 맞추려고 둔다. 여기 없는 게임은 서버 이름을 그대로 쓴다.
const TITLES = {
  worldcup: '음식 이상형 월드컵',
  balance: '음식 취향 밸런스 게임',
  roulette: '음식 룰렛',
}

export default function GamePage() {
  const [games, setGames] = useState([])
  const [recent, setRecent] = useState([]) // 최근 선택한 메뉴 (식사 기록)
  const [today, setToday] = useState(null) // 오늘의 게임 결과

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setGames(data.games || [])
      } catch (err) {
        // 못 받아오면 빈 목록
      }
    }
    load()

    // 게임 결과는 서버가 아니라 브라우저에 있다 (utils/gameResult.js)
    setToday(getTodayResult())
  }, [])

  // 최근 선택한 메뉴. 로그인했을 때만 받아온다
  useEffect(() => {
    if (!getToken()) {
      return
    }

    const load = async () => {
      try {
        const data = await getHistories({ page: 1, size: 3 })
        setRecent(data.items || [])
      } catch (err) {
        // 못 받아오면 그 칸만 안 보인다
      }
    }
    load()
  }, [])

  // 서버가 모르는 게임을 보내도 색이 비지 않게 기본값을 준다
  const toneOf = (key) => {
    if (TONES[key]) {
      return TONES[key]
    }
    return 'orange'
  }

  // 게임 이름. 예전에 저장된 '오늘의 게임 결과' 도 옛 이름 대신 새 이름으로 보인다
  const titleOf = (key, fallback) => {
    if (TITLES[key]) {
      return TITLES[key]
    }
    return fallback
  }

  return (
    <div className="list-page list-page--wide gpage">
      {/* ----- 배너 ----- */}
      {/* 오른쪽 그림은 public/images/game/banner.png 다. 없으면 배경만 보인다 */}
      <header className="gpage__hero">
        <div className="gpage__hero-text">
          <p className="gpage__kicker">FOOD GAME</p>
          <h2 className="gpage__title">오늘 뭐 먹지? 🎮</h2>
          <p className="gpage__lead">
            고민은 짧게, 선택은 재밌게.
            <br />
            3가지 게임으로 오늘의 메뉴를 결정해보세요.
          </p>

          {/* 오른쪽에 두면 배경 음식 그림과 겹친다. 설명 아래 왼쪽에 둔다 */}
          <p className="gpage__hand">게임으로 고른 메뉴가 바로 레시피로 이어져요!</p>
        </div>
      </header>

      {/* ----- 게임 카드 셋 ----- */}
      <ul className="gpage__games">
        {games.map((game, index) => (
          <li key={game.key}>
            <article className={`gpage__game gpage__game--${toneOf(game.key)}`}>
              <div className="gpage__game-head">
                {/* index 가 0 부터라 번호는 +1 한다 */}
                <span className="gpage__game-no">{index + 1}</span>
                <h3 className="gpage__game-title">{titleOf(game.key, game.title)}</h3>
              </div>

              <p className="gpage__game-desc">{game.desc}</p>

              {/* 그림은 public/images/game/{key}.png. 없으면 이모지가 대신 보인다 */}
              <span className="gpage__game-art">
                <span className="gpage__game-emoji">{game.emoji}</span>
                <img src={`/images/game/card-${game.key}.png`} alt="" />
              </span>

              <Link to={game.to} className="gpage__game-go">
                시작하기 →
              </Link>
            </article>
          </li>
        ))}
      </ul>

      {/* ----- 아래 두 칸 ----- */}
      <div className="gpage__bottom">
        {/* 오늘의 게임 결과 */}
        <section className="card gpage__result">
          <h3 className="gpage__box-title">🎉 오늘의 게임 결과</h3>

          {today ? (
            <div className="gpage__result-on">
              <p className="muted gpage__result-from">{titleOf(today.game_key, today.game_title)}에서 고른 메뉴</p>

              {/* 뽑힌 요리 사진. 룰렛은 요리가 아니라 분류(한식)를 뽑아서 사진을 안 그린다 */}
              {today.game_key !== 'roulette' && (
                <span className="gpage__result-thumb">
                  <FoodThumb item={today} size="md" />
                </span>
              )}

              <strong className="gpage__result-food">{today.food_name}</strong>
              <Link to={today.link} className="btn btn--sm gpage__result-go">
                레시피 보기 →
              </Link>
            </div>
          ) : (
            <div className="gpage__result-off">
              <span className="gpage__result-icon">📋</span>
              <p className="gpage__result-lead">아직 오늘의 메뉴를 정하지 못했어요.</p>
              <p className="muted gpage__result-desc">게임을 플레이하면 선택된 음식이 여기에 나타납니다.</p>
            </div>
          )}
        </section>

        {/* 최근 선택한 메뉴. 로그인해야 보인다 */}
        {recent.length > 0 && (
          <section className="card gpage__recent">
            <div className="section-head">
              <h3 className="gpage__box-title">🕐 최근 선택한 메뉴</h3>
              <Link to="/history" className="section-more">
                전체보기 →
              </Link>
            </div>

            <ul className="gpage__recent-list">
              {recent.map((item) => (
                <li key={item.id}>
                  <Link to={`/recipe/${item.food_name}`}>
                    <FoodThumb item={item} size="lg" />
                    <strong className="gpage__recent-name">{item.food_name}</strong>
                    <span className="muted gpage__recent-date">{formatDate(item.selected_at)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </div>
  )
}
