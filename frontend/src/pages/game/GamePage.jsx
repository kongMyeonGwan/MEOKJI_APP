import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCatalog } from '../../api/catalog'
import '../../styles/ListPage.css'
import './GamePage.css'

// 메뉴 게임 목록 (월드컵 / 밸런스 / 룰렛). 게임 목록은 서버에서 온다.
export default function GamePage() {
  const [games, setGames] = useState([])

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setGames(data.games)
      } catch (err) {
        // 못 받아오면 빈 목록
      }
    }
    load()
  }, [])

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">메뉴 게임</h2>
      </div>

      <p className="muted list-page__desc">고르기 어려운 날엔 놀면서 정해요. 결과는 바로 레시피로 이어집니다.</p>

      <ul className="game-list">
        {games.map((game) => (
          <li key={game.key}>
            <Link to={game.to} className="card game-list__item">
              <span className="game-list__emoji">{game.emoji}</span>
              <strong className="game-list__title">{game.title}</strong>
              <span className="game-list__desc">{game.desc}</span>
              <span className="btn btn--outline btn--block game-list__cta">시작하기 →</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
