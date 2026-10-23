import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodThumb from '../../components/common/FoodThumb'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { GAME_KEYS, recordGamePlay } from '../../api/games'
import { getFavorites, toggleFavorite } from '../../api/favorites'
import { addLog, foodNames, markPicked } from '../../utils/aiLog'
import { saveResult } from '../../utils/gameResult'
import { formatMinutes, formatPrice } from '../../utils/format'

// 음식 월드컵. 후보 8개를 둘씩 붙여서 하나를 고르면 다음 라운드로 올라간다.
//   8강(4번 대결) → 4강(2번) → 결승(1번) → 우승
//
// 상태 설명
//   round   : 이번 라운드에 남아 있는 메뉴들 (8개 → 4개 → 2개 → 1개)
//   index   : 지금 대결하는 두 메뉴 중 왼쪽 메뉴의 위치 (0, 2, 4, 6)
//   winners : 이번 라운드에서 이긴 메뉴들. 라운드가 끝나면 이게 다음 round 가 된다.

const ROUND_NAME = { 8: '8강', 4: '4강', 2: '결승' }

export default function WorldCupPage({ user }) {
  const navigate = useNavigate()

  const [round, setRound] = useState([])
  const [index, setIndex] = useState(0)
  const [winners, setWinners] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [liked, setLiked] = useState(false) // 우승 메뉴를 찜했는지

  // 후보 8개를 받아와서 대진표를 새로 짠다
  const startGame = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getRecommendations({ source: '메뉴 게임', limit: 8 })
      setRound(data.items || [])
      setIndex(0)
      setWinners([])
      addLog('음식 월드컵', data.condition, foodNames(data.items))
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

  // 처음 한 번: 게임 시작, 로그인했으면 게임 기록 남기기
  useEffect(() => {
    startGame()
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
      const champion = nextWinners[0].foodName
      markPicked(champion) // AI 추천 기록에 "이걸로 정함" 표시
      // 메뉴 게임 화면의 '오늘의 게임 결과' 에 띄울 값 (브라우저에만 저장한다)
      saveResult('worldcup', '음식 월드컵', champion, `/recipe/${champion}`)
      setLiked(false)
      checkLiked(nextWinners[0].foodName)
    }
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

  if (loading) {
    return (
      <div className="list-page list-page--narrow">
        <div className="card">
          <Loading message="AI가 후보를 고르는 중..." />
        </div>
      </div>
    )
  }

  // 후보를 못 받았거나 짝이 안 맞을 때
  if (error || round.length === 0 || (round.length > 1 && !round[index + 1])) {
    return (
      <div className="list-page list-page--narrow">
        <div className="card">
          <ErrorMessage message={error || '후보를 받아오지 못했습니다.'} />
          <button type="button" className="btn" onClick={startGame}>
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
      <div className="list-page list-page--narrow">
        <div className="card wc wc--done">
          <p className="wc__crown">🏆</p>
          <p className="wc__round">오늘의 우승 메뉴</p>
          <h2 className="wc__champion">{champion.foodName}</h2>

          <div className="wc__champion-thumb">
            <FoodThumb item={champion} size="lg" />
          </div>

          <p className="muted wc__champion-reason">{champion.reason}</p>
          <p className="wc__champion-meta">
            <span className="price">{formatPrice(champion.estimatedCost)}</span> ·{' '}
            {formatMinutes(champion.cookTimeMinutes)} · 난이도 {champion.difficulty}
          </p>

          <div className="wc__actions">
            <Link to={`/recipe/${champion.foodName}`} className="btn">
              레시피 보기
            </Link>
            <button type="button" className="btn btn--ghost" onClick={() => handleLike(champion.foodName)}>
              {liked ? '♥ 찜 해제' : '♡ 찜하기'}
            </button>
            <button type="button" className="btn btn--ghost" onClick={startGame}>
              다시 하기
            </button>
          </div>
        </div>
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
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">음식 월드컵</h2>
        <button type="button" className="section-more wc__restart" onClick={startGame}>
          처음부터 ↻
        </button>
      </div>

      <p className="wc__round">
        {roundName} · {matchNo} / {matchTotal}
      </p>

      <div className="wc__match">
        {[left, right].map((food, i) => (
          <button key={food.foodName} type="button" className="card wc__pick" onClick={() => choose(food)}>
            <FoodThumb item={food} size="lg" />
            <strong className="wc__pick-name">{food.foodName}</strong>
            <span className="muted wc__pick-reason">{food.reason}</span>
            <span className="wc__pick-meta">
              <span className="price">{formatPrice(food.estimatedCost)}</span> · {formatMinutes(food.cookTimeMinutes)}
            </span>
            {/* 두 카드 사이의 VS 글자 (왼쪽 카드에만 붙인다) */}
            {i === 0 && <span className="wc__vs">VS</span>}
          </button>
        ))}
      </div>
    </div>
  )
}
