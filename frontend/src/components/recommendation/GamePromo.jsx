import { Link } from 'react-router-dom'
import { MOCK_GAMES } from '../../mocks'
import '../../styles/components/recommendation/GamePromo.css'

/**
 * 메뉴 게임 배너.
 *
 * 히어로 바로 아래에 둔다. 위에서 "오늘은 콩국수"라고 정해줬는데도
 * 안 당기는 사람이 다음으로 하는 생각이 "그럼 뭐 먹지"라서,
 * 그 자리에서 고르는 걸 대신해주는 장치를 내미는 순서다.
 *
 * 세 게임을 같은 크기로 늘어놓는다. 여기서는 "골라보세요"가 맞다.
 * 셋의 성격이 달라서(비교 · 질문 · 운) 어느 게 끌리는지가 곧 선택이다.
 *
 * 카드 자체가 링크라 "게임 시작하기"는 없어도 되지만,
 * 셋 다 안 정해진 사람을 위해 게임 목록(/game)으로 가는 문을 하나 둔다.
 */
export default function GamePromo() {
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
        {MOCK_GAMES.map((game) => (
          <li key={game.key}>
            <Link to={game.to} className="gpromo__game">
              <span className="gpromo__game-emoji" aria-hidden="true">
                {game.emoji}
              </span>
              {/* 글자를 한 겹 더 감싼 이유: 좁은 화면에서는 카드가 가로로 눕는데,
                  감싸지 않으면 제목과 설명이 이모지 옆에 나란히 늘어선다 */}
              <span className="gpromo__game-text">
                <strong className="gpromo__game-title">{game.title}</strong>
                {/* 설명이 \n 으로 두 줄이다. CSS의 pre-line 이 그대로 살린다 */}
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
        <span className="gpromo__robot" aria-hidden="true">
          🤖
        </span>
      </div>
    </section>
  )
}
