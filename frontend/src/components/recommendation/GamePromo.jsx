import { Link } from 'react-router-dom'
import '../../styles/components/recommendation/GamePromo.css'

// 첫 화면의 메뉴 게임 배너.
//   games : [{ key, to, emoji, title, desc }, ...]
export default function GamePromo({ games }) {
  return (
    <section className="gpromo">
      <div className="gpromo__lead">
        <span className="gpromo__icon">🎮</span>
        <p className="gpromo__question">아직도 뭐 먹을지 고민되세요? 🤔</p>
        <h2 className="gpromo__title">
          재미있는 게임으로
          <br />
          오늘 메뉴를 골라보세요!
        </h2>
        <p className="gpromo__desc">AI가 준비한 메뉴들로 게임하면서 결정해요</p>
      </div>

      <ul className="gpromo__games">
        {games.map((game) => (
          <li key={game.key}>
            <Link to={game.to} className="gpromo__game">
              <span className="gpromo__game-emoji">{game.emoji}</span>
              <span className="gpromo__game-text">
                <strong className="gpromo__game-title">{game.title}</strong>
                <span className="gpromo__game-desc">{game.desc}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>

      <div className="gpromo__cta">
        <Link to="/game" className="btn">
          게임 시작하기 →
        </Link>
        <span className="gpromo__robot">🤖</span>
      </div>
    </section>
  )
}
