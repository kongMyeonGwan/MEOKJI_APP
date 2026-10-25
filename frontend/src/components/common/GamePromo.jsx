import { Link } from 'react-router-dom'

// 메뉴 게임 배너. 첫 화면(HomePage)과 로그인 화면(LoginPage)이 같이 쓴다.
// 두 화면이 쓰게 되면서 pages/home/components/ 에서 여기로 옮겼다.
//
//   games   : [{ key, to, emoji, title, desc }, ...] - 서버의 /api/catalog 가 준다
//   variant : 'compact'(기본) 첫 화면용. 이모지 카드 셋
//             'full'          로그인 화면용. 요리사 + 음식 사진 + 카드마다 버튼
//
// 두 모양이 글자와 링크를 똑같이 쓰기 때문에 컴포넌트를 둘로 나누지 않았다.
// 나누면 게임이 하나 늘 때 두 군데를 고쳐야 한다.

// full 에서만 쓰는 꾸밈. 서버가 주는 key 로 찾는다.
//   tone  : 카드 색 (주황 · 초록 · 보라)
//   cta   : 카드 아래 버튼 글자
//   shots : 카드 안에 놓을 그림. 둘이면 가운데에 VS 가 들어간다
const EXTRAS = {
  worldcup: { tone: 'orange', cta: '월드컵 시작하기', shots: ['chicken', 'pasta'] },
  balance: { tone: 'purple', cta: '밸런스 시작하기', shots: ['stew', 'salad'] },
  roulette: { tone: 'yellow', cta: '돌림판 돌리기', shots: ['ramen', 'roulette', 'burger'] },
}

export default function GamePromo({ games, variant = 'compact' }) {
  const full = variant === 'full'

  // 서버가 모르는 게임을 보내도 화면이 깨지지 않게 기본값을 돌려준다
  const extraOf = (key) => {
    if (EXTRAS[key]) {
      return EXTRAS[key]
    }
    return { tone: 'orange', cta: '시작하기', shots: [] }
  }

  // 카드 안의 그림들. 둘이면 사이에 VS 를 끼운다 (VS 는 그림이 아니라 CSS 로 그린 동그라미)
  const renderShots = (game) => {
    const shots = extraOf(game.key).shots

    if (shots.length === 0) {
      return null
    }

    return (
      <span className="gpromo__shots">
        {shots.map((shot, index) => (
          <span key={shot} className="gpromo__shot-slot">
            {/* 첫 장 다음에만, 그리고 두 장짜리일 때만 VS 를 넣는다 */}
            {index === 1 && shots.length === 2 && <span className="gpromo__vs">VS</span>}
            {/* 장식이라 alt 는 비워둔다. 읽어주면 오히려 방해가 된다 */}
            <img className="gpromo__shot" src={`/images/game/${shot}.png`} alt="" />
          </span>
        ))}
      </span>
    )
  }

  // 게임 한 장. compact 는 카드 전체가 링크, full 은 안에 버튼이 따로 있다
  // (링크 안에 링크를 넣을 수 없어서 바깥을 div 로 둔다)
  const renderGame = (game) => {
    const extra = extraOf(game.key)

    const body = (
      <>
        <span className="gpromo__game-emoji">{game.emoji}</span>
        <span className="gpromo__game-text">
          <strong className="gpromo__game-title">{game.title}</strong>
          <span className="gpromo__game-desc">{game.desc}</span>
        </span>
      </>
    )

    if (!full) {
      return (
        <li key={game.key}>
          <Link to={game.to} className="gpromo__game">
            {body}
          </Link>
        </li>
      )
    }

    return (
      <li key={game.key}>
        <div className={`gpromo__game gpromo__game--${extra.tone}`}>
          {body}
          {renderShots(game)}
          <Link to={game.to} className="gpromo__go">
            {extra.cta} →
          </Link>
        </div>
      </li>
    )
  }

  return (
    <section className={full ? 'gpromo gpromo--full' : 'gpromo'}>
      <div className="gpromo__lead">
        <span className="gpromo__icon">🎮</span>
        <p className="gpromo__question">아직도 뭐 먹을지 고민되세요? 🤔</p>
        <h2 className="gpromo__title">
          {/* 앱 모드에서는 <br> 을 숨겨 한 줄로 흐른다. 그때 두 말이 붙지 않게 띄어쓰기를 둔다 */}
          재미있는 게임으로{' '}
          <br />
          오늘 메뉴를 골라보세요!
        </h2>
        <p className="gpromo__desc">AI가 준비한 메뉴들로 게임하면서 결정해요</p>

        {/* full 에서는 시작 버튼과 요리사가 소개 안에 들어간다 */}
        {full && (
          <>
            <Link to="/game" className="btn gpromo__start">
              게임 시작하기 →
            </Link>
            <img className="gpromo__chef" src="/images/game/chef.png" alt="" />
          </>
        )}
      </div>

      <ul className="gpromo__games">{games.map(renderGame)}</ul>

      {/* compact 에서는 시작 버튼이 오른쪽 끝에 따로 선다 */}
      {!full && (
        <div className="gpromo__cta">
          <Link to="/game" className="btn">
            게임 시작하기 →
          </Link>
          <span className="gpromo__robot">🤖</span>
        </div>
      )}
    </section>
  )
}
