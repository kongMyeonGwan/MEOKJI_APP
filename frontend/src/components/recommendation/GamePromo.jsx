import { Link } from 'react-router-dom'
import { useCatalog } from '../../contexts/CatalogContext'
import '../../styles/components/recommendation/GamePromo.css'

// 메뉴 게임 배너. 첫 화면의 히어로 바로 아래에 둔다.
// 위에서 "오늘은 콩국수" 라고 정해줘도 안 당기는 사람이 많아서
// 그 자리에서 고르는 걸 대신해주는 게임을 바로 내민다.
//
// 세 게임의 성격이 달라서(비교 / 질문 / 운) 같은 크기로 늘어놓는다.
export default function GamePromo() {
  const { games } = useCatalog()

  return (
    <section className="gpromo" aria-label="메뉴 게임">
      <div className="gpromo__lead">
        <span className="gpromo__icon" aria-hidden="true">
          🎮
        </span>
        <p className="gpromo__question">
          아직도 뭐 먹을지 고민되세요? <span aria-hidden="true">🤔</span>
        </p>
        <h2 className="gpromo__title">
          재미있는 게임으로
          <br />
          오늘 메뉴를 골라보세요!
        </h2>
        <p className="gpromo__desc">AI가 준비한 메뉴들로 게임하면서 결정해요</p>
      </div>

      <ul className="gpromo__games">
        {games.map(function (game) {
          return (
            <li key={game.key}>
              <Link to={game.to} className="gpromo__game">
                <span className="gpromo__game-emoji" aria-hidden="true">
                  {game.emoji}
                </span>
                {/* 글자를 한 번 더 감쌌다. 좁은 화면에서 카드가 가로로 눕는데
                    안 감싸면 제목이랑 설명이 이모지 옆에 나란히 늘어선다. */}
                <span className="gpromo__game-text">
                  <strong className="gpromo__game-title">{game.title}</strong>
                  {/* 설명이 \n 으로 두 줄이다. CSS 의 pre-line 이 살려준다. */}
                  <span className="gpromo__game-desc">{game.desc}</span>
                </span>
              </Link>
            </li>
          )
        })}
      </ul>

      <div className="gpromo__cta">
        <Link to="/game" className="btn">
          게임 시작하기 →
        </Link>
        <span className="gpromo__robot" aria-hidden="true">
          🤖
        </span>
      </div>
    </section>
  )
}
