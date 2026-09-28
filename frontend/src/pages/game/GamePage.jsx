import { Link } from 'react-router-dom'
import { useCatalog } from '../../contexts/CatalogContext'
import '../../styles/pages/ListPage.css'
import '../../styles/pages/GamePage.css'

// 메뉴 게임 목록.
//
// 세 게임 다 "못 고르겠다" 를 푸는 도구인데 푸는 방식이 다르다.
//   월드컵 - 후보를 둘씩 비교해서 좁힌다. 시간은 걸리는데 납득이 된다.
//   밸런스 - 질문 다섯 개로 조건을 만든다. 뭐가 당기는지 자기도 모를 때.
//   돌림판 - 운에 맡긴다. 제일 빠르다.
//
// 첫 화면의 배너(GamePromo)랑 같은 목록을 쓴다.
// 게임을 하나 더 붙일 때 서버의 catalog.json 만 고치면 두 화면이 같이 바뀐다.
export default function GamePage() {
  const { games } = useCatalog()

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">메뉴 게임</h2>
      </div>

      <p className="muted list-page__desc">
        고르기 어려운 날엔 놀면서 정해요. 결과는 바로 레시피로 이어집니다.
      </p>

      <ul className="game-list">
        {games.map(function (game) {
          return (
            <li key={game.key}>
              <Link to={game.to} className="card game-list__item">
                <span className="game-list__emoji" aria-hidden="true">
                  {game.emoji}
                </span>
                <strong className="game-list__title">{game.title}</strong>
                <span className="game-list__desc">{game.desc}</span>
                <span className="btn btn--outline btn--block game-list__cta">
                  시작하기 →
                </span>
              </Link>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
