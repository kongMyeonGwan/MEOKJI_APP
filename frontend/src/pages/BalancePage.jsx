import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import FoodCard from '../components/recommendation/FoodCard'
import Loading from '../components/common/Loading'
import { getRecommendations } from '../api/recommendations'
import { GAME_KEYS, recordGamePlay } from '../api/games'
import { useAiLog } from '../contexts/AiLogContext'
import { useAuth } from '../contexts/AuthContext'
import { useCatalog } from '../contexts/CatalogContext'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/ListPage.css'
import '../styles/pages/GamePage.css'

// 취향 밸런스 게임.
//
// 질문 다섯 개로 "지금 당기는 조건" 을 만든다.
// 예산이나 시간을 입력칸으로 물으면 답하기 전에 생각을 해야 하는데
// 둘 중 하나 고르기는 생각 없이 눌린다.
//
// 고른 답을 점수로 바꾸지 않고 문장으로 만들어서 AI 한테 넘긴다.
// 화면에서 점수를 매기려면 후보 목록 전체를 들고 있어야 하고,
// 다섯 조건을 다 만족하는 음식은 거의 없어서 결국 애매한 답이 된다.
//
// 질문은 서버에서 온다. 질문을 하나 더 붙일 때 catalog.json 만 고치면 된다.

// 결과로 보여줄 개수. 하나만 주면 안 당길 때 빠져나갈 데가 없다.
const RESULT_COUNT = 3

// 고른 답들을 AI 한테 넘길 한 문장으로 만든다.
//   "뜨끈한 국물, 얼큰하고 매콤하게, 면이 당기는 날, 20분 안에 끝내기"
// 답의 label 을 그대로 잇는다. 사람이 읽는 말이라 AI 도 그대로 이해한다.
function makeQuery(answers) {
  const labels = []

  for (let i = 0; i < answers.length; i++) {
    labels.push(answers[i].label)
  }

  return labels.join(', ')
}

// 고른 답에서 예산 상한을 찾는다. 없으면 null.
// 예산만 문장이 아니라 조건으로 따로 넘긴다.
// 금액은 안 지키면 바로 티가 나서 서버가 확실히 거르게 하는 게 낫다.
function findMaxCost(answers) {
  for (let i = 0; i < answers.length; i++) {
    if (answers[i].maxCost) {
      return answers[i].maxCost
    }
  }
  return null
}

// 고른 답에서 예산 하한을 찾는다. 없으면 null.
function findMinCost(answers) {
  for (let i = 0; i < answers.length; i++) {
    if (answers[i].minCost) {
      return answers[i].minCost
    }
  }
  return null
}

export default function BalancePage() {
  const { isAuthenticated } = useAuth()

  // 두 번 기록되지 않게 막는다.
  // 개발 모드에서는 리액트가 useEffect 를 일부러 두 번 실행한다.
  const logged = useRef(false)

  useEffect(() => {
    if (!isAuthenticated || logged.current) {
      return
    }

    logged.current = true
    recordGamePlay(GAME_KEYS.BALANCE)
  }, [isAuthenticated])

  // 지금까지 고른 답들. 길이가 곧 몇 번째 질문인지가 된다.
  const [answers, setAnswers] = useState([])

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')

  const { balanceQuestions } = useCatalog()
  const { add } = useAiLog()

  const step = answers.length
  const total = balanceQuestions.length
  const done = total > 0 && step >= total

  // 마지막 질문에 답했을 때 서버에 추천을 요청한다
  function finish(allAnswers) {
    setLoading(true)
    setLoadError('')

    const query = makeQuery(allAnswers)

    getRecommendations({
      source: '메뉴 게임',
      limit: RESULT_COUNT,
      query: query,
      budgetMin: findMinCost(allAnswers),
      budgetMax: findMaxCost(allAnswers),
    })
      .then(function (data) {
        setItems(data.items)

        const names = []
        for (let i = 0; i < data.items.length; i++) {
          names.push(data.items[i].food_name)
        }

        add({ source: '취향 밸런스', condition: query, foods: names })
      })
      .catch(function (err) {
        setLoadError(parseApiError(err).message)
      })
      .finally(function () {
        setLoading(false)
      })
  }

  // 답 하나를 고른다. 마지막 답이면 바로 결과를 받아온다.
  function answer(option) {
    const next = answers.concat([option])
    setAnswers(next)

    if (next.length >= total) {
      finish(next)
    }
  }

  function restart() {
    setAnswers([])
    setItems([])
    setLoadError('')
  }

  // 이전 질문으로 돌아간다
  function goBack() {
    setAnswers(answers.slice(0, answers.length - 1))
  }

  // 질문을 아직 못 받았을 때 (서버가 안 떠 있거나 로딩 중)
  if (total === 0) {
    return (
      <div className="list-page list-page--narrow">
        <div className="card">
          <p className="empty">질문을 불러오는 중…</p>
        </div>
      </div>
    )
  }

  // 질문을 다 풀었으면 결과 화면
  if (done) {
    return (
      <div className="list-page">
        <div className="section-head">
          <h2 className="section-title">이런 메뉴는 어떠세요?</h2>
          <button
            type="button"
            className="section-more wc__restart"
            onClick={restart}
          >
            다시 하기 ↻
          </button>
        </div>

        <p className="muted list-page__desc">고르신 답: {makeQuery(answers)}</p>

        {loadError && <ErrorMessage message={loadError} />}

        {!loadError && loading && (
          <Loading message="AI가 취향에 맞는 메뉴를 고르는 중..." />
        )}

        {!loadError && !loading && items.length === 0 && (
          <p className="empty">
            조건에 맞는 메뉴를 찾지 못했습니다.
            <br />
            <button type="button" className="btn btn--sm" onClick={restart}>
              다시 해보기
            </button>
          </p>
        )}

        {!loadError && !loading && items.length > 0 && (
          <div className="list-page__cards">
            {items.map(function (food) {
              return <FoodCard key={food.food_name} food={food} />
            })}
          </div>
        )}

        <p className="muted balance__foot">
          마음에 드는 게 없다면 <Link to="/game/worldcup">음식 월드컵</Link>으로
          골라보셔도 좋아요.
        </p>
      </div>
    )
  }

  // 질문 화면
  const question = balanceQuestions[step]
  const progressWidth = (step / total) * 100 + '%'

  return (
    <div className="list-page list-page--narrow">
      <div className="section-head">
        <h2 className="section-title">취향 밸런스</h2>
        <span className="muted section-more">
          {step + 1} / {total}
        </span>
      </div>

      {/* 몇 개 남았는지 보여야 중간에 그만두지 않는다 */}
      <div className="balance__progress">
        <span style={{ width: progressWidth }} />
      </div>

      <p className="balance__question">{question.question}</p>

      <div className="balance__options">
        {question.options.map(function (option) {
          return (
            <button
              key={option.key}
              type="button"
              className="card balance__option"
              onClick={() => answer(option)}
            >
              <span className="balance__emoji" aria-hidden="true">
                {option.emoji}
              </span>
              <strong className="balance__label">{option.label}</strong>
            </button>
          )
        })}
      </div>

      {step > 0 && (
        <button
          type="button"
          className="btn btn--ghost btn--sm balance__back"
          onClick={goBack}
        >
          ← 이전 질문
        </button>
      )}
    </div>
  )
}
