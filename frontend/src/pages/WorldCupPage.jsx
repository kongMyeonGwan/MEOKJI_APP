import { useState } from 'react'
import { Link } from 'react-router-dom'
import FoodThumb from '../components/common/FoodThumb'
import { useLikes } from '../contexts/LikesContext'
import { MOCK_RECOMMENDATIONS } from '../mocks'
import { formatMinutes, formatPrice } from '../utils/format'
import '../styles/pages/ListPage.css'
import '../styles/pages/GamePage.css'

/**
 * 음식 월드컵.
 *
 * 후보를 둘씩 붙여 하나씩 떨어뜨린다. 여덟 개를 한 화면에 늘어놓고
 * "고르세요" 하면 오래 걸리는데, 둘 중 하나는 0.5초면 고를 수 있다.
 * 세 번 고르면 답이 나온다.
 *
 * 8강으로 시작하는 이유: 16강은 열다섯 번을 눌러야 해서 중간에 지치고,
 * 4강은 두 번 만에 끝나 고른 것 같지가 않다.
 *
 * 후보는 매번 섞는다. 같은 순서로 나오면 두 번째부터는 결과가 뻔해진다.
 * 서버가 붙으면 이 자리에 날씨·못 먹는 재료를 반영한 후보가 내려온다.
 */

/** 대진표 크기. 2의 거듭제곱이어야 부전승 없이 딱 떨어진다 */
const BRACKET_SIZE = 8

const ROUND_NAME = { 8: '8강', 4: '4강', 2: '결승' }

function drawBracket() {
  // 원본을 건드리지 않도록 복사한 뒤 섞는다
  const shuffled = [...MOCK_RECOMMENDATIONS].sort(() => Math.random() - 0.5)
  return shuffled.slice(0, BRACKET_SIZE)
}

export default function WorldCupPage() {
  const [round, setRound] = useState(drawBracket)
  // 지금 화면에 띄운 두 명이 round의 몇 번째인지
  const [index, setIndex] = useState(0)
  const [winners, setWinners] = useState([])

  const { has, toggle } = useLikes()

  const restart = () => {
    setRound(drawBracket())
    setIndex(0)
    setWinners([])
  }

  const choose = (food) => {
    const nextWinners = [...winners, food]

    // 이번 라운드의 마지막 대결이면 이긴 사람들로 다음 라운드를 연다
    if (index + 2 >= round.length) {
      setRound(nextWinners)
      setWinners([])
      setIndex(0)
      return
    }

    setWinners(nextWinners)
    setIndex(index + 2)
  }

  // ===== 우승 =====
  if (round.length === 1) {
    const champion = round[0]
    const liked = has(champion.food_name)

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
              to={`/recipe/${encodeURIComponent(champion.food_name)}`}
              className="btn"
            >
              레시피 보기
            </Link>
            <button
              type="button"
              className="btn btn--ghost"
              onClick={() => toggle(champion.food_name)}
              aria-pressed={liked}
            >
              {liked ? '♥ 찜 해제' : '♡ 찜하기'}
            </button>
            <button type="button" className="btn btn--ghost" onClick={restart}>
              다시 하기
            </button>
          </div>
        </div>
      </div>
    )
  }

  // ===== 대결 중 =====
  const [left, right] = [round[index], round[index + 1]]
  const matchNo = index / 2 + 1
  const matchTotal = round.length / 2

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">음식 월드컵</h2>
        <button type="button" className="section-more wc__restart" onClick={restart}>
          처음부터 ↻
        </button>
      </div>

      <p className="wc__round">
        {ROUND_NAME[round.length] ?? `${round.length}강`} · {matchNo} / {matchTotal}
      </p>

      <div className="wc__match">
        {[left, right].map((food, i) => (
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

            {/* 두 카드 사이에 놓이는 VS. 왼쪽 카드에만 붙인다 */}
            {i === 0 && (
              <span className="wc__vs" aria-hidden="true">
                VS
              </span>
            )}
          </button>
        ))}
      </div>
    </div>
  )
}
