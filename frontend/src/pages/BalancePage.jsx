import { useState } from 'react'
import { Link } from 'react-router-dom'
import FoodCard from '../components/recommendation/FoodCard'
import { MOCK_BALANCE_QUESTIONS, MOCK_RECOMMENDATIONS } from '../mocks'
import '../styles/pages/ListPage.css'
import '../styles/pages/GamePage.css'

/**
 * 취향 밸런스.
 *
 * 질문 다섯 개로 "지금 당기는 조건"을 만든다. 예산·시간 같은 걸 입력칸으로
 * 물으면 답하기 전에 생각을 해야 하는데, 둘 중 하나 고르기는 생각 없이 눌린다.
 *
 * 고른 답이 곧 필터다. 조건을 다 만족하는 메뉴만 남기지 않고 **점수로 세운다**.
 * 다섯 개를 모두 만족하는 음식은 거의 없어서, 전부 걸러버리면 결과가 0건이 된다.
 *
 * 점수 계산이 화면에 있는 건 임시다. 서버가 붙으면 고른 답을 그대로 넘겨
 * AI가 조건에 맞는 메뉴를 만들어 내려준다(mocks의 질문 형식이 그 요청 모양이다).
 */

/** 결과로 보여줄 개수. 하나만 주면 안 당길 때 빠져나갈 데가 없다 */
const RESULT_COUNT = 3

/** 답 하나가 음식 한 건에 몇 점을 주는지 */
function scoreOne(food, option) {
  let point = 0

  if (option.tags?.some((tag) => food.tags?.includes(tag))) point += 2
  if (option.maxCost && food.estimated_cost <= option.maxCost) point += 1
  if (option.minCost && food.estimated_cost >= option.minCost) point += 1
  if (option.maxTime && food.cook_time_minutes <= option.maxTime) point += 1
  if (option.minTime && food.cook_time_minutes >= option.minTime) point += 1

  return point
}

export default function BalancePage() {
  const [answers, setAnswers] = useState([])

  const step = answers.length
  const total = MOCK_BALANCE_QUESTIONS.length
  const done = step >= total

  const restart = () => setAnswers([])

  // ===== 결과 =====
  if (done) {
    const results = [...MOCK_RECOMMENDATIONS]
      .map((food) => ({
        food,
        point: answers.reduce((sum, option) => sum + scoreOne(food, option), 0),
      }))
      // 점수가 같으면 싼 것을 위로. 기준이 없으면 순서가 매번 달라 보인다
      .sort((a, b) => b.point - a.point || a.food.estimated_cost - b.food.estimated_cost)
      .slice(0, RESULT_COUNT)

    return (
      <div className="list-page">
        <div className="section-head">
          <h2 className="section-title">이런 메뉴는 어떠세요?</h2>
          <button type="button" className="section-more wc__restart" onClick={restart}>
            다시 하기 ↻
          </button>
        </div>

        <p className="muted list-page__desc">
          고르신 답:{' '}
          {answers.map((option, i) => (
            <span key={MOCK_BALANCE_QUESTIONS[i].id}>
              {i > 0 && ' · '}
              {option.label}
            </span>
          ))}
        </p>

        <div className="list-page__cards">
          {results.map(({ food }) => (
            <FoodCard key={food.food_name} food={food} />
          ))}
        </div>

        <p className="muted balance__foot">
          마음에 드는 게 없다면 <Link to="/game/worldcup">음식 월드컵</Link>으로
          골라보셔도 좋아요.
        </p>
      </div>
    )
  }

  // ===== 질문 =====
  const question = MOCK_BALANCE_QUESTIONS[step]

  return (
    <div className="list-page list-page--narrow">
      <div className="section-head">
        <h2 className="section-title">취향 밸런스</h2>
        <span className="muted section-more">
          {step + 1} / {total}
        </span>
      </div>

      {/* 몇 개 남았는지 보여야 중간에 그만두지 않는다 */}
      <div
        className="balance__progress"
        role="progressbar"
        aria-valuenow={step}
        aria-valuemin={0}
        aria-valuemax={total}
      >
        <span style={{ width: `${(step / total) * 100}%` }} />
      </div>

      <p className="balance__question">{question.question}</p>

      <div className="balance__options">
        {question.options.map((option) => (
          <button
            key={option.key}
            type="button"
            className="card balance__option"
            onClick={() => setAnswers((prev) => [...prev, option])}
          >
            <span className="balance__emoji" aria-hidden="true">
              {option.emoji}
            </span>
            <strong className="balance__label">{option.label}</strong>
          </button>
        ))}
      </div>

      {step > 0 && (
        <button
          type="button"
          className="btn btn--ghost btn--sm balance__back"
          onClick={() => setAnswers((prev) => prev.slice(0, -1))}
        >
          ← 이전 질문
        </button>
      )}
    </div>
  )
}
