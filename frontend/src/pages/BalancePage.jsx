import { useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../components/common/ErrorMessage'
import FoodCard from '../components/recommendation/FoodCard'
import Loading from '../components/common/Loading'
import { getRecommendations } from '../api/recommendations'
import { GAME_KEYS } from '../api/games'
import { useGamePlayLog } from '../hooks/useGamePlayLog'
import { useAiLog } from '../contexts/AiLogContext'
import { useCatalog } from '../contexts/CatalogContext'
import { parseApiError } from '../utils/apiError'
import '../styles/pages/ListPage.css'
import '../styles/pages/GamePage.css'

/**
 * 취향 밸런스.
 *
 * 질문 다섯 개로 "지금 당기는 조건"을 만든다. 예산·시간 같은 걸 입력칸으로
 * 물으면 답하기 전에 생각을 해야 하는데, 둘 중 하나 고르기는 생각 없이 눌린다.
 *
 * 고른 답을 점수로 바꾸지 않고 **문장으로 만들어 AI에게 넘긴다.**
 * 화면에서 점수를 매기려면 후보 목록 전체를 들고 있어야 하고, 다섯 조건을
 * 모두 만족하는 음식은 거의 없어서 결국 "점수 높은 순"이라는 애매한 답이 된다.
 * 조건을 그대로 넘기면 AI 가 그 조건에 맞는 메뉴를 처음부터 만들어 준다.
 *
 * 질문은 서버에서 온다(CatalogContext). 질문을 하나 더 붙일 때
 * catalog.json 만 고치면 이 화면이 그대로 따라온다.
 */

/** 결과로 보여줄 개수. 하나만 주면 안 당길 때 빠져나갈 데가 없다 */
const RESULT_COUNT = 3

/**
 * 고른 답들을 AI에게 넘길 한 문장으로 만든다.
 *
 *   "뜨끈한 국물, 얼큰하고 매콤하게, 면이 당기는 날, 20분 안에 끝내기"
 *
 * 답의 label 을 그대로 잇는다. 사람이 읽는 말이라 AI 도 그대로 이해한다.
 * 태그·시간 같은 내부 값을 따로 넘기지 않아도 되고, 질문이 늘어나도
 * 이 함수를 고칠 일이 없다.
 */
function makeQuery(answers) {
  const labels = []

  for (const option of answers) {
    labels.push(option.label)
  }

  return labels.join(', ')
}

/**
 * 고른 답에서 예산 상한을 찾는다. 없으면 null.
 *
 * 예산만 문장이 아니라 조건으로 따로 넘긴다.
 * 금액은 지키지 못하면 사용자가 바로 알아채는 조건이라, 문장으로 부탁하는
 * 것보다 서버가 확실히 거르게 하는 편이 낫다.
 */
function findMaxCost(answers) {
  for (const option of answers) {
    if (option.maxCost) {
      return option.maxCost
    }
  }
  return null
}

/** 고른 답에서 예산 하한을 찾는다. 없으면 null. */
function findMinCost(answers) {
  for (const option of answers) {
    if (option.minCost) {
      return option.minCost
    }
  }
  return null
}

export default function BalancePage() {
  // 이 게임을 시작했다고 서버에 한 번 남긴다 (로그인했을 때만)
  useGamePlayLog(GAME_KEYS.BALANCE)

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

  /** 마지막 질문에 답했을 때 서버에 추천을 요청한다. */
  function finish(allAnswers) {
    setLoading(true)
    setLoadError('')

    const query = makeQuery(allAnswers)

    getRecommendations({
      source: '메뉴 게임',
      limit: RESULT_COUNT,
      query,
      budgetMin: findMinCost(allAnswers),
      budgetMax: findMaxCost(allAnswers),
    })
      .then((data) => {
        setItems(data.items)

        const names = []
        for (const item of data.items) {
          names.push(item.food_name)
        }
        add({ source: '취향 밸런스', condition: query, foods: names })
      })
      .catch((err) => setLoadError(parseApiError(err).message))
      .finally(() => setLoading(false))
  }

  /** 답 하나를 고른다. 마지막 답이면 바로 결과를 받아온다. */
  function answer(option) {
    const next = [...answers, option]
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

  // ===== 질문을 다 풀었으면 결과 화면 =====
  if (done) {
    return (
      <div className="list-page">
        <div className="section-head">
          <h2 className="section-title">이런 메뉴는 어떠세요?</h2>
          <button type="button" className="section-more wc__restart" onClick={restart}>
            다시 하기 ↻
          </button>
        </div>

        <p className="muted list-page__desc">
          고르신 답: {makeQuery(answers)}
        </p>

        {loadError ? (
          <ErrorMessage message={loadError} />
        ) : loading ? (
          <Loading message="AI가 취향에 맞는 메뉴를 고르는 중..." />
        ) : items.length === 0 ? (
          <p className="empty">
            조건에 맞는 메뉴를 찾지 못했습니다.
            <br />
            <button type="button" className="btn btn--sm" onClick={restart}>
              다시 해보기
            </button>
          </p>
        ) : (
          <div className="list-page__cards">
            {items.map((food) => (
              <FoodCard key={food.food_name} food={food} />
            ))}
          </div>
        )}

        <p className="muted balance__foot">
          마음에 드는 게 없다면 <Link to="/game/worldcup">음식 월드컵</Link>으로
          골라보셔도 좋아요.
        </p>
      </div>
    )
  }

  // ===== 질문 =====
  const question = balanceQuestions[step]

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
            onClick={() => answer(option)}
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
