import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import FoodCard from '../../components/common/FoodCard'
import Icon from '../../components/common/Icon'
import Loading from '../../components/common/Loading'
import { getRecommendations } from '../../api/recommendations'
import { GAME_KEYS, recordGamePlay } from '../../api/games'
import { addLog, foodNames } from '../../utils/aiLog'
import { saveResult } from '../../utils/gameResult'
import RoundPicker from './components/RoundPicker'
import BALANCE_QUESTIONS from './components/balanceQuestions'

// 음식 취향 밸런스 게임.
// 시작 전에 16강 · 8강 · 4강(질문 16 · 8 · 4개)을 고른다.
// 둘 중 하나 고르는 질문에 다 답하면, 고른 답의 낱말(keyword)을 이어서 AI 에게 추천받는다.
//   예) "국물 얼큰 면"
//
// 질문은 서버가 아니라 components/balanceQuestions.js 에 있다 (서버에는 5개뿐이라서).

// 고를 수 있는 질문 수
const COUNTS = [
  { value: 16, label: '16강 (질문 16개)' },
  { value: 8, label: '8강 (질문 8개)' },
  { value: 4, label: '4강 (질문 4개)' },
]

// 고르는 화면의 안내 문장. 질문 하나에 5초쯤 걸린다고 보고 시간을 어림한다
const describeCount = (value) => {
  const seconds = value * 5
  return `질문 ${BALANCE_QUESTIONS.length}개 중 무작위 ${value}개가 나옵니다. 약 ${seconds}초면 끝나요.`
}

// 고르는 화면의 진행 순서
const countSteps = (value) => {
  return [`질문 ${value}개`, '취향 분석', '추천 메뉴 3개']
}

// 보기 사진. public/images/game/balance/q{질문id}-{A|B}.jpg 규칙이다.
// 파일이 없거나 못 읽으면 이모지를 대신 보여준다
function OptionPicture({ questionId, option }) {
  const [failed, setFailed] = useState(false)

  if (failed) {
    return <span className="balance__emoji">{option.emoji}</span>
  }

  return (
    <img
      className="balance__img"
      src={`/images/game/balance/q${questionId}-${option.key}.jpg`}
      alt={option.label}
      onError={() => setFailed(true)}
    />
  )
}

// 서버가 검색어를 150자까지만 받는다
const MAX_QUERY = 150

// 질문 count 개를 무작위로 뽑는다. 순서는 원래 순서를 지킨다
const pickQuestions = (count) => {
  const ids = BALANCE_QUESTIONS.map((q) => q.id)
  for (let i = ids.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[ids[i], ids[j]] = [ids[j], ids[i]]
  }
  const picked = ids.slice(0, count)
  return BALANCE_QUESTIONS.filter((q) => picked.includes(q.id))
}

export default function BalancePage({ user }) {
  const [questions, setQuestions] = useState([]) // 이번 판의 질문들. 비어 있으면 고르는 화면
  const [answers, setAnswers] = useState([]) // 지금까지 고른 답들
  const [items, setItems] = useState([]) // 추천 결과
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // 처음 한 번: 로그인했으면 게임 기록을 남긴다
  useEffect(() => {
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

  // AI 에게 보낼 검색어. 화면용 문장(label)은 길어서 짧은 낱말(keyword)만 잇는다
  //   [{ keyword: '국물' }, { keyword: '얼큰' }] → "국물 얼큰"
  const makeKeywords = (list) => {
    const words = list.map((answer) => answer.keyword).filter((word) => word)
    return words.join(' ').slice(0, MAX_QUERY)
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
        query: makeKeywords(allAnswers),
        budgetMin: budgetAnswer ? budgetAnswer.minCost : null,
        budgetMax: budgetAnswer ? budgetAnswer.maxCost : null,
      })
      setItems(data.items || [])
      addLog('음식 취향 밸런스 게임', makeQuery(allAnswers), foodNames(data.items))

      // 메뉴 게임 화면의 '오늘의 게임 결과' 에 띄울 값. 셋 중 첫 번째(가장 잘 맞는 것)를 쓴다
      if (data.items && data.items.length > 0) {
        const top = data.items[0].foodName
        saveResult('balance', '음식 취향 밸런스 게임', top, `/recipe/${top}`)
      }
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

  // 질문 수를 골랐을 때
  const handleSelectCount = (value) => {
    setQuestions(pickQuestions(value))
    setAnswers([])
    setItems([])
    setError('')
  }

  // 질문 수 고르는 화면으로 돌아간다
  const restart = () => {
    setQuestions([])
    setAnswers([])
    setItems([])
    setError('')
  }

  // 아직 질문 수를 안 골랐으면 고르는 화면
  if (questions.length === 0) {
    return (
      <RoundPicker
        icon="⚖️"
        title="음식 취향 밸런스 게임"
        tags={['둘 중 하나만 고르면 끝', '많이 답할수록 정확하게', 'AI가 메뉴 3개 추천']}
        label="총 라운드를 선택하세요."
        choices={COUNTS}
        defaultValue={8}
        describe={describeCount}
        steps={countSteps}
        onStart={handleSelectCount}
      />
    )
  }

  // 질문을 다 풀었으면 결과 화면
  if (answers.length === questions.length) {
    return (
      // --wide : 카드 격자라 폭을 묶지 않고 본문을 꽉 채운다
      <div className="list-page list-page--wide game-screen">
        <div className="section-head">
          <h2 className="section-title">이런 메뉴는 어떠세요?</h2>
          <div className="game-head-actions">
            <Link to="/game" className="game-btn">
              <Icon name="gamepad" className="game-icon-svg" />
              게임 목록
            </Link>
            <button type="button" className="game-btn" onClick={restart}>
              <span className="game-icon game-icon--restart" aria-hidden="true" />
              다시 하기
            </button>
          </div>
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
              <FoodCard key={food.foodName} food={food} />
            ))}
          </div>
        )}

        <p className="muted balance__foot">
          마음에 드는 게 없다면 <Link to="/game/worldcup">음식 이상형 월드컵</Link>으로 골라보셔도 좋아요.
        </p>
      </div>
    )
  }

  // 질문 화면
  const step = answers.length // 지금 몇 번째 질문인지 (0부터)
  const question = questions[step]

  return (
    // balance-screen : 질문 · 보기 · 이전 질문이 한 화면에 다 들어오게 간격과 사진 높이를 줄인다
    <div className="list-page list-page--narrow game-screen balance-screen">
      <div className="section-head">
        <h2 className="section-title">음식 취향 밸런스 게임</h2>
        <div className="balance__head-side">
          <span className="muted section-more">
            {step + 1} / {questions.length}
          </span>
          <Link to="/game" className="game-btn">
            <Icon name="gamepad" className="game-icon-svg" />
            게임 목록
          </Link>
          <button type="button" className="game-btn" onClick={restart}>
            <span className="game-icon game-icon--restart" aria-hidden="true" />
            처음부터 다시하기
          </button>
        </div>
      </div>

      {/* 진행 막대 */}
      <div className="balance__progress">
        <span style={{ width: `${(step / questions.length) * 100}%` }} />
      </div>

      <p className="balance__question">{question.question}</p>

      <div className="balance__options">
        {question.options.map((option) => (
          <button key={option.key} type="button" className="card balance__option" onClick={() => chooseAnswer(option)}>
            <OptionPicture key={`${question.id}-${option.key}`} questionId={question.id} option={option} />
            <strong className="balance__label">{option.label}</strong>
          </button>
        ))}

        {/* 두 보기 사이의 VS. 보기를 눌러도 가려지지 않게 맨 위에 그린다 */}
        <span className="balance__vs" aria-hidden="true">
          VS
        </span>
      </div>

      {step > 0 && (
        <div className="game-bottom">
          <button type="button" className="game-btn" onClick={goBack}>
            <span className="game-icon game-icon--back" aria-hidden="true" />
            이전 질문
          </button>
        </div>
      )}
    </div>
  )
}
