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
//
// 대신 치르는 것 — full 과 compact 를 가리는 if 가 네 군데 생겼다.
// 모양이 더 갈라지면 나누는 쪽이 나을 수 있는 자리다.
//
// games 는 서버에서 받은 것을 그대로 받는다. 게임 목록을 코드에
// 적어두지 않은 이유 — 제목과 설명을 고칠 때 배포를 다시 해야 한다.
// 다만 카드 색과 그림은 화면 사정이라 아래 EXTRAS 에 적어두었다.

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
  // 아래에서 여러 번 보므로 한 번만 가려 변수로 둔다
  const full = variant === 'full'

  // 서버가 모르는 게임을 보내도 화면이 깨지지 않게 기본값을 돌려준다
  //
  // EXTRAS[key] 를 바로 쓰면 새 게임이 추가됐을 때 undefined 가 되고,
  // .tone 을 읽는 순간 터진다. 게임은 서버가 정하고 꾸밈은 여기
  // 적혀 있으니, 서로 어긋날 수 있는 자리다
  const extraOf = (key) => {
    if (EXTRAS[key]) {
      return EXTRAS[key]
    }
    return { tone: 'orange', cta: '시작하기', shots: [] }
  }

  // 카드 안의 그림들. 둘이면 사이에 VS 를 끼운다 (VS 는 그림이 아니라 CSS 로 그린 동그라미)
  const renderShots = (game) => {
    const shots = extraOf(game.key).shots

    // 기본값으로 떨어진 게임은 그림이 없다. null 을 돌려주면
    // 그 자리를 아예 안 그린다
    if (shots.length === 0) {
      return null
    }

    return (
      <span className="gpromo__shots">
        {shots.map((shot, index) => (
          <span key={shot} className="gpromo__shot-slot">
            {/* 첫 장 다음에만, 그리고 두 장짜리일 때만 VS 를 넣는다.
                월드컵 · 밸런스는 두 장이라 VS 가 어울리는데,
                룰렛은 세 장이라 어디에 넣어도 어색하다 */}
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
  //
  // 넣으면 브라우저가 알아서 고쳐버려서, 화면에 그려지는 모양이
  // 코드와 달라진다 - 누르면 어디로 가는지도 예측할 수 없다
  const renderGame = (game) => {
    const extra = extraOf(game.key)

    // 두 모양이 같이 쓰는 안쪽. 변수로 빼두면 아래에서 두 번
    // 적지 않아도 된다
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
          재미있는 게임으로
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

      {/* renderGame 을 그대로 넘긴다. (game) => renderGame(game) 로
          적어도 같지만, 함수 이름만 넘기는 쪽이 짧다.
          key 는 renderGame 안에서 <li> 에 붙인다 */}
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
