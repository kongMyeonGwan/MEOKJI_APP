import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodThumb from '../../components/common/FoodThumb'
import Icon from '../../components/common/Icon'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { GAME_KEYS, recordGamePlay } from '../../api/games'
import { getFavorites, toggleFavorite } from '../../api/favorites'
import { recordWorldcupWin } from '../../api/worldcup'
import { addLog, foodNames, markPicked } from '../../utils/aiLog'
import { saveResult } from '../../utils/gameResult'
import { formatMinutes, formatPrice } from '../../utils/format'
import RoundPicker from './components/RoundPicker'
import Confetti from './components/Confetti'
import HallOfFameButton from './components/HallOfFameButton'

// 음식 이상형 월드컵. 후보를 둘씩 붙여서 하나를 고르면 다음 라운드로 올라간다.
// 시작 전에 64강 · 32강 · 16강 중 하나를 고른다.
//   64강(32번 대결) → 32강 → 16강 → 8강 → 4강 → 결승(1번) → 우승
//
// 상태 설명
//   size    : 고른 대진 크기 (64 · 32 · 16). 아직 안 골랐으면 null → 고르는 화면
//   round   : 이번 라운드에 남아 있는 메뉴들 (64개 → 32개 → ... → 2개 → 1개)
//   index   : 지금 대결하는 두 메뉴 중 왼쪽 메뉴의 위치 (0, 2, 4, 6 ...)
//   winners : 이번 라운드에서 이긴 메뉴들. 라운드가 끝나면 이게 다음 round 가 된다.

const ROUND_NAME = { 64: '64강', 32: '32강', 16: '16강', 8: '8강', 4: '4강', 2: '결승' }

// 대진표는 둘씩 짝을 지어 반씩 줄어야 해서 후보 수가 2의 거듭제곱이어야 한다.
// 못 먹는 재료 등으로 후보가 64개보다 적게 오면 가능한 가장 큰 수(32, 16...)로 줄인다.
const MAX_CANDIDATES = 64

// 고를 수 있는 대진 크기
const SIZES = [
  { value: 64, label: '64강' },
  { value: 32, label: '32강' },
  { value: 16, label: '16강' },
]

// 고르는 화면의 안내 문장. 대결 수 = 후보 수 - 1 (한 번 질 때마다 하나씩 떨어진다)
const describeSize = (value) => {
  return `요리 100종 중 무작위 ${value}개가 대결합니다. 총 ${value - 1}번 고르면 우승 메뉴가 나와요.`
}

// 고르는 화면의 진행 순서. 64 → ['64강', '32강', ..., '4강', '결승']
const sizeSteps = (value) => {
  const steps = []
  for (let n = value; n >= 2; n = n / 2) {
    steps.push(ROUND_NAME[n])
  }
  return steps
}

// 서버는 점수 순으로 주기 때문에 그대로 붙이면 매번 같은 대진표가 나온다. 섞어서 쓴다.
const shuffle = (list) => {
  const copy = [...list]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

// 추천 API 는 한 번에 12개까지만 주고, exclude 도 30개까지만 받는다.
// 그래서 받은 메뉴를 전부 exclude 로 넘기는 대신 '가격' 으로 넘겨 가며 모은다.
//   조건 없이 부르면 서버가 싼 순으로 주므로, 이번에 받은 가장 비싼 값을
//   다음 번 최소 금액(budgetMin)으로 넘긴다. 그 금액과 값이 같은 메뉴만 exclude 로 뺀다.
// 64개가 모이거나 더 받을 게 없으면 멈춘다. (100종이면 7~8번 부른다)
const PAGE_SIZE = 12
const MAX_EXCLUDE = 30

const fetchCandidates = async () => {
  let items = []
  let condition = ''
  let minCost = 0

  while (items.length < MAX_CANDIDATES) {
    // 이미 받은 것 중 이번 최소 금액 이상인 메뉴만 다시 나올 수 있다
    const seen = items
      .filter((food) => food.estimated_cost >= minCost)
      .map((food) => food.food_name)
      .slice(-MAX_EXCLUDE)

    const data = await getRecommendations({
      source: '메뉴 게임',
      limit: PAGE_SIZE,
      budgetMin: minCost,
      exclude: seen,
    })
    // 혹시 이미 받은 메뉴가 또 오면 뺀다
    const names = items.map((food) => food.food_name)
    const page = (data.items || []).filter((food) => !names.includes(food.food_name))

    if (items.length === 0) {
      condition = data.condition // 첫 번째 것만 기록에 남긴다 (금액 조건이 안 붙은 문구)
    }
    if (page.length === 0) {
      break // 더 받을 메뉴가 없다
    }

    items = [...items, ...page]
    minCost = Math.max(...page.map((food) => food.estimated_cost))
  }

  return { items: items, condition: condition }
}

// 16강을 골라도 후보는 64개를 다 받아서 그중에서 뽑는다.
// 처음 받은 것만 쓰면 서버가 싼 순으로 주기 때문에 싼 메뉴만 나온다.
const makeBracket = (items, wantSize) => {
  let size = wantSize
  while (size > items.length && size > 2) {
    size = size / 2
  }
  return shuffle(items).slice(0, size)
}

export default function WorldCupPage({ user }) {
  const navigate = useNavigate()

  const [size, setSize] = useState(null)
  const [round, setRound] = useState([])
  const [index, setIndex] = useState(0)
  const [winners, setWinners] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [liked, setLiked] = useState(false) // 우승 메뉴를 찜했는지
  // 명예의 전당을 다시 받아오게 하는 값. 이번 우승이 저장되면 1 늘려서
  // 우승 화면의 순위표가 방금 판까지 센 순위로 새로 뜨게 한다
  const [rankKey, setRankKey] = useState(0)

  // 후보를 받아와서 고른 크기만큼 대진표를 새로 짠다
  const startGame = async (bracketSize) => {
    setLoading(true)
    setError('')
    try {
      const data = await fetchCandidates()
      const bracket = makeBracket(data.items, bracketSize)
      setRound(bracket)
      setIndex(0)
      setWinners([])
      addLog('음식 이상형 월드컵', data.condition, foodNames(bracket))
    } catch (err) {
      // 서버가 보낸 오류 문장을 그대로 보여준다.
      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          setError(detail[0].msg)
        } else {
          setError(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  // 처음 한 번: 로그인했으면 게임 기록 남기기. 게임은 크기를 고르면 시작한다
  useEffect(() => {
    if (user) {
      recordGamePlay(GAME_KEYS.WORLDCUP)
    }
  }, [])

  // 우승 메뉴를 이미 찜해뒀는지 확인한다
  const checkLiked = async (foodName) => {
    if (!user) {
      return
    }
    try {
      const data = await getFavorites()
      setLiked(data.names.includes(foodName))
    } catch (err) {
      // 몰라도 괜찮다
    }
  }

  // 두 메뉴 중 하나를 골랐을 때
  const choose = (food) => {
    const nextWinners = [...winners, food]

    // 이번 라운드의 마지막 대결이 아니면 다음 대결로
    if (index + 2 < round.length) {
      setWinners(nextWinners)
      setIndex(index + 2)
      return
    }

    // 라운드가 끝났으면 이긴 메뉴들로 다음 라운드 시작
    setRound(nextWinners)
    setWinners([])
    setIndex(0)

    // 우승이 정해졌으면
    if (nextWinners.length === 1) {
      const champion = nextWinners[0].food_name
      markPicked(champion) // AI 추천 기록에 "이걸로 정함" 표시
      // 메뉴 게임 화면의 '오늘의 게임 결과' 에 띄울 값 (브라우저에만 저장한다)
      saveResult('worldcup', '음식 이상형 월드컵', champion, `/recipe/${champion}`)

      // 명예의 전당에 남긴다 (로그인했을 때만). 결승에서 진 쪽도 같이 보내 결승 진출 횟수를 센다
      if (user) {
        const runnerUp = food === round[index] ? round[index + 1] : round[index]
        recordWorldcupWin({ foodName: champion, runnerUp: runnerUp.food_name, roundSize: size }).then(() => {
          setRankKey((key) => key + 1)
        })
      }
      setLiked(false)
      checkLiked(nextWinners[0].food_name)
    }
  }

  // 크기를 골랐을 때
  const handleSelectSize = (value) => {
    setSize(value)
    startGame(value)
  }

  // 크기 고르는 화면으로 돌아간다
  const backToPicker = () => {
    setSize(null)
    setRound([])
    setIndex(0)
    setWinners([])
    setError('')
  }

  const handleLike = async (foodName) => {
    if (!user) {
      navigate('/login')
      return
    }
    try {
      const result = await toggleFavorite(foodName)
      setLiked(result.favorited)
    } catch (err) {
      alert('찜하지 못했습니다.')
    }
  }

  // 아직 크기를 안 골랐으면 고르는 화면. 카드 오른쪽 위에 명예의 전당 바로가기 버튼을 둔다
  if (size === null) {
    return (
      <RoundPicker
        icon="🏆"
        title="음식 이상형 월드컵"
        tags={['요리 100종', '사진 · 가격 · 조리시간 비교', '우승 메뉴는 바로 레시피로']}
        label="총 라운드를 선택하세요."
        choices={SIZES}
        defaultValue={32}
        describe={describeSize}
        steps={sizeSteps}
        onStart={handleSelectSize}
        corner={<HallOfFameButton />}
      />
    )
  }

  if (loading) {
    return (
      <div className="list-page list-page--narrow game-screen">
        <div className="card">
          <Loading message="AI가 후보를 고르는 중..." />
        </div>
      </div>
    )
  }

  // 후보를 못 받았거나 짝이 안 맞을 때
  if (error || round.length === 0 || (round.length > 1 && !round[index + 1])) {
    return (
      <div className="list-page list-page--narrow game-screen">
        <div className="card">
          <ErrorMessage message={error || '후보를 받아오지 못했습니다.'} />
          <button type="button" className="btn" onClick={() => startGame(size)}>
            다시 시도
          </button>
        </div>
      </div>
    )
  }

  // 우승 화면
  if (round.length === 1) {
    const champion = round[0]

    return (
      <div className="list-page list-page--narrow game-screen">
        <div className="card wc wc--done">
          {/* 오른쪽 위: 명예의 전당 바로가기. 이번 우승이 저장되면 순위를 새로 받아온다 */}
          <div className="wc__done-corner">
            <HallOfFameButton highlight={champion.food_name} refreshKey={rankKey} />
          </div>

          <p className="wc__crown">🏆</p>
          <p className="wc__round">오늘의 우승 메뉴</p>
          <h2 className="wc__champion">{champion.food_name}</h2>

          <div className="wc__champion-thumb">
            <FoodThumb item={champion} size="lg" />
          </div>

          <p className="muted wc__champion-reason">{champion.reason}</p>
          <p className="wc__champion-meta">
            <span className="price">{formatPrice(champion.estimated_cost)}</span> ·{' '}
            {formatMinutes(champion.cook_time_minutes)} · 난이도 {champion.difficulty}
          </p>

          <div className="wc__actions">
            <Link to={`/recipe/${champion.food_name}`} className="btn">
              레시피 보기
            </Link>
            <button type="button" className="btn btn--ghost" onClick={() => handleLike(champion.food_name)}>
              {liked ? '♥ 찜 해제' : '♡ 찜하기'}
            </button>
            <button type="button" className="btn btn--ghost game-btn--plain" onClick={backToPicker}>
              <span className="game-icon game-icon--restart" aria-hidden="true" />
              다시 하기
            </button>
            <Link to="/game" className="btn btn--ghost game-btn--plain">
              <Icon name="gamepad" className="game-icon-svg" />
              게임 목록
            </Link>
          </div>
        </div>

        {/* 우승 화면이 뜰 때 한 번 폭죽을 터뜨린다. 다시 해서 또 우승하면 화면이 새로 떠서 또 터진다 */}
        <Confetti fire={1} />
      </div>
    )
  }

  // 대결 화면
  const left = round[index]
  const right = round[index + 1]
  const roundName = ROUND_NAME[round.length] || `${round.length}강`
  const matchNo = index / 2 + 1 // 몇 번째 대결인지
  const matchTotal = round.length / 2 // 이번 라운드의 대결 수

  return (
    <div className="list-page game-screen game-screen--wide">
      <div className="section-head">
        <h2 className="section-title">음식 이상형 월드컵</h2>
        {/* 게임 목록(/game) · 라운드 고르는 화면으로 돌아간다 */}
        <div className="game-head-actions">
          <Link to="/game" className="game-btn">
            <Icon name="gamepad" className="game-icon-svg" />
            게임 목록
          </Link>
          <button type="button" className="game-btn" onClick={backToPicker}>
            <span className="game-icon game-icon--restart" aria-hidden="true" />
            처음부터 다시하기
          </button>
        </div>
      </div>

      <p className="wc__round">
        {roundName} · {matchNo} / {matchTotal}
      </p>

      <div className="wc__match">
        {[left, right].map((food, i) => (
          <button key={food.food_name} type="button" className="card wc__pick" onClick={() => choose(food)}>
            <FoodThumb item={food} size="lg" />
            <strong className="wc__pick-name">{food.food_name}</strong>
            <span className="muted wc__pick-reason">{food.reason}</span>
            <span className="wc__pick-meta">
              <span className="price">{formatPrice(food.estimated_cost)}</span> · {formatMinutes(food.cook_time_minutes)}
            </span>
            {/* 두 카드 사이의 VS 글자 (왼쪽 카드에만 붙인다) */}
            {i === 0 && <span className="wc__vs">VS</span>}
          </button>
        ))}
      </div>
    </div>
  )
}
