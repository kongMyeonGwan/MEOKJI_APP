import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodCard from '../../components/common/FoodCard'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { getCatalog } from '../../api/catalog'
import { GAME_KEYS, recordGamePlay } from '../../api/games'
import { addLog, foodNames } from '../../utils/aiLog'
import '../../styles/ListPage.css'
import './GamePage.css'

// 취향 밸런스 게임.
// 둘 중 하나 고르는 질문 5개에 답하면, 고른 답을 문장으로 이어서 AI 에게 추천받는다.
//   예) "뜨끈한 국물, 얼큰하고 매콤하게, 면이 당기는 날"
//
// 질문 모양 (서버에서 온다)
//   { question: '오늘은?', options: [{ key, emoji, label, minCost?, maxCost? }, ...] }
export default function BalancePage({ user }) {
  const [questions, setQuestions] = useState([])
  const [answers, setAnswers] = useState([]) // 지금까지 고른 답들
  const [items, setItems] = useState([]) // 추천 결과
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // 처음 한 번: 질문 목록을 받아오고, 로그인했으면 게임 기록을 남긴다
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setQuestions(data.balance_questions)
      } catch (err) {
        setError('질문을 불러오지 못했습니다.')
      }
    }
    load()

    if (user) {
      recordGamePlay(GAME_KEYS.BALANCE)
    }
  }, [])

  // 고른 답들을 한 문장으로 잇는다
  //   [{ label: '뜨끈한 국물' }, { label: '매콤하게' }] → "뜨끈한 국물, 매콤하게"
  const makeQuery = (list) => {
    const labels = list.map((answer) => answer.label)
    return labels.join(', ')
  }

  // 마지막 질문까지 답하면 AI 에게 추천을 받는다
  const getResult = async (allAnswers) => {
    // 예산이 들어 있는 답이 있으면 금액 조건도 같이 보낸다
    const budgetAnswer = allAnswers.find((answer) => answer.minCost || answer.maxCost)

    setLoading(true)
    setError('')
    try {
      const data = await getRecommendations({
        source: '메뉴 게임',
        limit: 3,
        query: makeQuery(allAnswers),
        budgetMin: budgetAnswer ? budgetAnswer.minCost : null,
        budgetMax: budgetAnswer ? budgetAnswer.maxCost : null,
      })
      setItems(data.items)
      addLog('취향 밸런스', makeQuery(allAnswers), foodNames(data.items))
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

  // 답 하나를 골랐을 때
  const chooseAnswer = (option) => {
    const nextAnswers = [...answers, option]
    setAnswers(nextAnswers)

    if (nextAnswers.length === questions.length) {
      getResult(nextAnswers)
    }
  }

  // 이전 질문으로
  const goBack = () => {
    setAnswers(answers.slice(0, answers.length - 1)) // 마지막 답 하나를 뺀다
  }

  const restart = () => {
    setAnswers([])
    setItems([])
    setError('')
  }

  // 질문을 아직 못 받았을 때
  if (questions.length === 0) {
    return (
      <div className="list-page list-page--narrow">
        <div className="card">
          {error ? <ErrorMessage message={error} /> : <p className="empty">질문을 불러오는 중…</p>}
        </div>
      </div>
    )
  }

  // 질문을 다 풀었으면 결과 화면
  if (answers.length === questions.length) {
    return (
      <div className="list-page">
        <div className="section-head">
          <h2 className="section-title">이런 메뉴는 어떠세요?</h2>
          <button type="button" className="section-more wc__restart" onClick={restart}>
            다시 하기 ↻
          </button>
        </div>

        <p className="muted list-page__desc">고르신 답: {makeQuery(answers)}</p>

        {loading && <Loading message="AI가 취향에 맞는 메뉴를 고르는 중..." />}
        {error && <ErrorMessage message={error} />}

        {!loading && !error && items.length === 0 && (
          <p className="empty">
            조건에 맞는 메뉴를 찾지 못했습니다.
            <br />
            <button type="button" className="btn btn--sm" onClick={restart}>
              다시 해보기
            </button>
          </p>
        )}

        {!loading && !error && items.length > 0 && (
          <div className="list-page__cards">
            {items.map((food) => (
              <FoodCard key={food.food_name} food={food} />
            ))}
          </div>
        )}

        <p className="muted balance__foot">
          마음에 드는 게 없다면 <Link to="/game/worldcup">음식 월드컵</Link>으로 골라보셔도 좋아요.
        </p>
      </div>
    )
  }

  // 질문 화면
  const step = answers.length // 지금 몇 번째 질문인지 (0부터)
  const question = questions[step]

  return (
    <div className="list-page list-page--narrow">
      <div className="section-head">
        <h2 className="section-title">취향 밸런스</h2>
        <span className="muted section-more">
          {step + 1} / {questions.length}
        </span>
      </div>

      {/* 진행 막대 */}
      <div className="balance__progress">
        <span style={{ width: `${(step / questions.length) * 100}%` }} />
      </div>

      <p className="balance__question">{question.question}</p>

      <div className="balance__options">
        {question.options.map((option) => (
          <button key={option.key} type="button" className="card balance__option" onClick={() => chooseAnswer(option)}>
            <span className="balance__emoji">{option.emoji}</span>
            <strong className="balance__label">{option.label}</strong>
          </button>
        ))}
      </div>

      {step > 0 && (
        <button type="button" className="btn btn--ghost btn--sm balance__back" onClick={goBack}>
          ← 이전 질문
        </button>
      )}
    </div>
  )
}
