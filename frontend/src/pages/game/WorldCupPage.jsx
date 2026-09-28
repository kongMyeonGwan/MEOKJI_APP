import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodThumb from '../../components/common/FoodThumb'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { GAME_KEYS, recordGamePlay } from '../../api/games'
import { useAiLog } from '../../contexts/AiLogContext'
import { useAuth } from '../../contexts/AuthContext'
import { useLikes } from '../../contexts/LikesContext'
import { parseApiError } from '../../utils/apiError'
import { formatMinutes, formatPrice } from '../../utils/format'
import '../../styles/pages/ListPage.css'
import '../../styles/pages/GamePage.css'

// 음식 월드컵.
//
// 후보를 둘씩 붙여서 하나씩 떨어뜨린다.
// 여덟 개를 한 화면에 늘어놓고 고르라고 하면 오래 걸리는데
// 둘 중 하나는 금방 고른다. 세 번 고르면 답이 나온다.
//
// 8강으로 시작하는 이유는 16강은 열다섯 번을 눌러야 해서 중간에 지치고
// 4강은 두 번 만에 끝나서 고른 것 같지가 않아서다.
//
// 후보는 서버에서 받아온다. 못 먹는 재료가 반영된 목록이라
// 알레르기 재료가 들어간 메뉴는 대진표에 안 오른다.

// 대진표 크기. 2의 거듭제곱이어야 부전승 없이 딱 떨어진다.
const BRACKET_SIZE = 8

const ROUND_NAME = { 8: '8강', 4: '4강', 2: '결승' }

export default function WorldCupPage() {
  const { isAuthenticated } = useAuth()
  const { has, toggle } = useLikes()
  const navigate = useNavigate()
  const location = useLocation()

  // 두 번 기록되지 않게 막는다.
  // 개발 모드에서는 리액트가 useEffect 를 일부러 두 번 실행한다.
  const logged = useRef(false)

  useEffect(() => {
    if (!isAuthenticated || logged.current) {
      return
    }

    logged.current = true
    recordGamePlay(GAME_KEYS.WORLDCUP)
  }, [isAuthenticated])

  // 이번 라운드에 남아있는 메뉴들 (8개 -> 4개 -> 2개 -> 1개)
  const [round, setRound] = useState([])

  // 지금 화면에 띄운 두 메뉴가 round 배열의 몇 번째인지 (0, 2, 4, 6)
  const [index, setIndex] = useState(0)

  // 이번 라운드에서 이긴 메뉴들. 라운드가 끝나면 이게 다음 round 가 된다.
  const [winners, setWinners] = useState([])

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const { add, markPicked } = useAiLog()

  // 하트를 눌렀을 때. 로그인 안 했으면 로그인 화면으로 보낸다.
  function onLike(foodName) {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }

    toggle(foodName)
  }

  // 후보를 새로 받아와서 대진표를 짠다
  function draw() {
    setLoading(true)
    setLoadError('')

    getRecommendations({ source: '메뉴 게임', limit: BRACKET_SIZE })
      .then(function (data) {
        setRound(data.items)
        setIndex(0)
        setWinners([])

        const names = []
        for (let i = 0; i < data.items.length; i++) {
          names.push(data.items[i].food_name)
        }

        add({
          source: '음식 월드컵',
          condition: data.items.length + '강 · ' + data.condition,
          foods: names,
        })
      })
      .catch(function (err) {
        setLoadError(parseApiError(err).message)
      })
      .finally(function () {
        setLoading(false)
      })
  }

  // 화면이 처음 열릴 때 한 번만 후보를 받아온다
  useEffect(() => {
    draw()
  }, [])

  // 두 카드 중 하나를 눌렀을 때. 고른 쪽이 다음 라운드로 올라간다.
  function choose(food) {
    const nextWinners = winners.concat([food])

    // 이번 라운드의 마지막 대결이었으면 이긴 메뉴들로 다음 라운드를 시작한다
    if (index + 2 >= round.length) {
      // 우승이 정해졌으면 AI 추천 기록에도 표시한다
      if (nextWinners.length === 1) {
        markPicked(nextWinners[0].food_name)
      }

      setRound(nextWinners) // 8강 승자 4명 -> 4강
      setWinners([])
      setIndex(0)
      return
    }

    // 아직 라운드가 안 끝났으면 다음 대결로 넘어간다.
    // 한 대결에 두 명씩이라 2칸씩 이동한다.
    setWinners(nextWinners)
    setIndex(index + 2)
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

  if (loadError || round.length === 0) {
    let errorText = loadError
    if (!errorText) {
      errorText = '후보를 받아오지 못했습니다.'
    }

    return (
      <div className="list-page list-page--narrow">
        <div className="card">
          <ErrorMessage message={errorText} />
          <button type="button" className="btn" onClick={draw}>
            다시 시도
          </button>
        </div>
      </div>
    )
  }

  // 우승 화면
  if (round.length === 1) {
    const champion = round[0]
    const liked = has(champion.food_name)

    let likeLabel = '♡ 찜하기'
    if (liked) {
      likeLabel = '♥ 찜 해제'
    }

    return (
      <div className="list-page list-page--narrow">
        <div className="card wc wc--done">
          <p className="wc__crown" aria-hidden="true">
            🏆
          </p>
          <p className="wc__round">오늘의 우승 메뉴</p>
          <h2 className="wc__champion">{champion.food_name}</h2>

          <div className="wc__champion-thumb">
            <FoodThumb item={champion} size="lg" />
          </div>

          <p className="muted wc__champion-reason">{champion.reason}</p>
          <p className="wc__champion-meta">
            <span className="price">{formatPrice(champion.estimated_cost)}</span>
            <span aria-hidden="true"> · </span>
            {formatMinutes(champion.cook_time_minutes)}
            <span aria-hidden="true"> · </span>
            난이도 {champion.difficulty}
          </p>

          <div className="wc__actions">
            <Link
              to={'/recipe/' + encodeURIComponent(champion.food_name)}
              className="btn"
            >
              레시피 보기
            </Link>
            <button
              type="button"
              className="btn btn--ghost"
              onClick={() => onLike(champion.food_name)}
            >
              {likeLabel}
            </button>
            <button type="button" className="btn btn--ghost" onClick={draw}>
              다시 하기
            </button>
          </div>
        </div>
      </div>
    )
  }

  // 대결 중
  const left = round[index]
  const right = round[index + 1]

  // 후보가 홀수로 오면 마지막 한 명이 짝을 못 만난다
  if (!right) {
    return (
      <div className="list-page list-page--narrow">
        <div className="card">
          <p className="empty">
            후보가 부족해 대진표를 짤 수 없습니다.
            <br />
            <button type="button" className="btn btn--sm" onClick={draw}>
              다시 뽑기
            </button>
          </p>
        </div>
      </div>
    )
  }

  // 한 대결에 두 명씩이라 index 를 2로 나누면 몇 번째 대결인지 나온다
  const matchNo = index / 2 + 1
  const matchTotal = Math.floor(round.length / 2)

  let roundName = ROUND_NAME[round.length]
  if (!roundName) {
    roundName = round.length + '강'
  }

  const match = [left, right]

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">음식 월드컵</h2>
        <button
          type="button"
          className="section-more wc__restart"
          onClick={draw}
        >
          처음부터 ↻
        </button>
      </div>

      <p className="wc__round">
        {roundName} · {matchNo} / {matchTotal}
      </p>

      <div className="wc__match">
        {match.map(function (food, i) {
          return (
            <button
              key={food.food_name}
              type="button"
              className="card wc__pick"
              onClick={() => choose(food)}
            >
              <FoodThumb item={food} size="lg" />
              <strong className="wc__pick-name">{food.food_name}</strong>
              <span className="muted wc__pick-reason">{food.reason}</span>
              <span className="wc__pick-meta">
                <span className="price">{formatPrice(food.estimated_cost)}</span>
                <span aria-hidden="true"> · </span>
                {formatMinutes(food.cook_time_minutes)}
              </span>

              {/* 두 카드 사이에 놓이는 VS. 왼쪽 카드에만 붙인다. */}
              {i === 0 && (
                <span className="wc__vs" aria-hidden="true">
                  VS
                </span>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
