import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../../components/common/FoodThumb'
import { getCatalog } from '../../api/catalog'
import { getGameStats } from '../../api/gameStats'
import { getTodayResult } from '../../utils/gameResult'

// 메뉴 게임 목록 (월드컵 / 밸런스 / 룰렛).
//   위   : 배너
//   가운데: 게임 카드 셋
//   아래 : [게임 플레이 통계][오늘의 게임 결과]  같은 높이로 나란히 둔다.
//          왼쪽은 전체 회원, 오른쪽은 나의 결과다
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

// 순위 왕관. 색은 CSS 가 순위(1 · 2 · 3)에 따라 금 · 은 · 동으로 칠한다
function Crown({ rank }) {
  return (
    <svg className={`gpage__crown gpage__crown--${rank}`} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 8.5l4.6 3.6L12 4.5l4.4 7.6L21 8.5 19.4 18H4.6z" />
      <rect x="4.6" y="19.2" width="14.8" height="2.3" rx="1" />
    </svg>
  )
}

export default function GamePage() {
  const [games, setGames] = useState([])
  const [stats, setStats] = useState(null) // 게임별 플레이 횟수 { items, total }. 못 받으면 null
  const [today, setToday] = useState(null) // 오늘의 게임 결과. 오늘 게임을 안 했으면 null

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

    // 전체 회원이 게임마다 몇 번 했는지 (GAME_HISTORIES 를 센다)
    const loadStats = async () => {
      try {
        setStats(await getGameStats())
      } catch (err) {
        // 못 받아오면 통계 칸에 안내 문구만 보인다
      }
    }
    loadStats()

    // 게임 결과는 서버가 아니라 브라우저에 있다 (utils/gameResult.js)
    setToday(getTodayResult())
  }, [])

  // 서버가 모르는 게임을 보내도 색이 비지 않게 기본값을 준다
  const toneOf = (key) => {
    if (TONES[key]) {
      return TONES[key]
    }
    return 'orange'
  }

  // 게임 이름. 서버 이름보다 각 게임 화면의 제목을 먼저 쓴다
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
        {/* 게임 플레이 통계. 많이 한 순서로 줄을 세우고 1 · 2 · 3등에 왕관을 단다.
            막대 길이는 가장 많이 한 게임을 100% 로 잡는다 */}
        <section className="card gpage__stats">
          <h3 className="gpage__box-title">📊 게임 플레이 통계</h3>
          <p className="muted gpage__stats-desc">
            {stats ? `모든 회원이 지금까지 ${stats.total.toLocaleString()}번 플레이했어요.` : '모든 회원의 게임 플레이 횟수예요.'}
          </p>

          {stats ? (
            <ul className="gpage__stats-list">
              {/* sort 는 원래 순서를 지키므로 횟수가 같으면 월드컵 · 밸런스 · 룰렛 순서로 놓인다 */}
              {[...stats.items].sort((a, b) => b.plays - a.plays).map((item) => {
                const max = Math.max(...stats.items.map((one) => one.plays), 1)
                const share = stats.total > 0 ? Math.round((item.plays / stats.total) * 100) : 0
                // 나보다 많이 한 게임 수 + 1. 횟수가 같으면 같은 등수다. 한 번도 안 했으면 등수가 없다
                const rank = item.plays > 0 ? stats.items.filter((one) => one.plays > item.plays).length + 1 : 0
                return (
                  <li key={item.game_key} className={`gpage__stat gpage__stat--${toneOf(item.game_key)}`} title={`${titleOf(item.game_key, item.game_key)} ${item.plays.toLocaleString()}회 (${share}%)`}>
                    <span className="gpage__stat-rank">
                      {rank >= 1 && rank <= 3 && <Crown rank={rank} />}
                      <span className="gpage__stat-rank-no">{rank > 0 ? `${rank}위` : '-'}</span>
                    </span>
                    <span className="gpage__stat-name">{titleOf(item.game_key, item.game_key)}</span>
                    <span className="gpage__stat-track">
                      <span className="gpage__stat-bar" style={{ width: `${(item.plays / max) * 100}%` }} />
                    </span>
                    <strong className="gpage__stat-count">{item.plays.toLocaleString()}회</strong>
                  </li>
                )
              })}
            </ul>
          ) : (
            <p className="muted gpage__stats-empty">통계를 불러오지 못했어요.</p>
          )}
        </section>

        {/* 오늘의 게임 결과 */}
        <section className="card gpage__result">
          <h3 className="gpage__box-title">🎉 오늘의 게임 결과</h3>

          {today ? (
            <div className="gpage__result-on">
              <p className="muted gpage__result-from">{titleOf(today.game_key, today.game_title)}에서 고른 메뉴</p>

              {/* 뽑힌 요리 사진. 룰렛이 메뉴를 못 고르고 분류(한식)만 남겼으면 사진을 안 그린다 */}
              {today.link.startsWith('/recipe/') && (
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
              <p className="muted gpage__result-desc">게임을 하면 고른 메뉴가 여기에 나타나요.</p>

              {/* 게임으로 바로 가기 */}
              <div className="gpage__result-start">
                {games.map((game) => (
                  <Link key={game.key} to={game.to} className={`gpage__result-chip gpage__result-chip--${toneOf(game.key)}`}>
                    {game.emoji} {titleOf(game.key, game.title)}
                  </Link>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
