import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { getCatalog } from '../../api/catalog'
import ErrorMessage from '../../components/common/ErrorMessage'
import GamePromo from '../../components/common/GamePromo'
import PasswordInput from '../../components/common/PasswordInput'
import useForm from '../../hooks/useForm'

// 로그인 화면.
//   위  : [왼쪽 소개][오른쪽 로그인 카드]
//   아래: 메뉴 게임 배너 (첫 화면과 같은 부품)
//
// 로그인 순서
//   1. 이메일, 비밀번호를 입력했는지 확인한다
//   2. 서버에 보낸다 → 서버가 { accessToken: '토큰', user: { 내 정보 } } 를 준다
//   3. onLogin 으로 App 에 알려준다 → App 이 브라우저에 저장하고 화면을 바꾼다
//   4. 홈으로 이동한다
//
// onLogin 은 App.jsx 가 props 로 넘겨준 함수다.

// 왼쪽에 늘어놓는 소개 셋
const POINTS = [
  { emoji: '🍴', title: '맞춤 추천', desc: '취향에 딱 맞는 메뉴' },
  { emoji: '👨‍🍳', title: '간편 레시피', desc: '누구나 쉽게 따라해요' },
  { emoji: '🧺', title: '예산 맞춤', desc: '가격까지 고려한 추천' },
]

export default function LoginPage({ onLogin }) {
  const { values, handleChange } = useForm({ email: '', password: '' })
  const [error, setError] = useState('') // 화면에 보여줄 에러 문장
  const [submitting, setSubmitting] = useState(false) // 서버 응답을 기다리는 중인지
  const [keep, setKeep] = useState(true) // 로그인 상태 유지
  const [games, setGames] = useState([]) // 아래 게임 배너에 쓸 목록

  const emailRef = useRef(null)
  const passwordRef = useRef(null)

  const navigate = useNavigate()

  // 화면이 열리면 이메일 칸에 커서
  useEffect(() => {
    emailRef.current.focus()
  }, [])

  // 아래 게임 배너에 쓸 목록을 받아온다. 로그인 없이도 부를 수 있는 API 다.
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCatalog()
        setGames(data.games || [])
      } catch (err) {
        // 못 받아오면 게임 배너만 안 보인다. 로그인은 그대로 된다
      }
    }
    load()
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    // 1. 입력 확인
    // 비어 있는 칸이 있으면 그 칸으로 커서를 옮긴다
    if (values.email.trim() === '') {
      setError('이메일을 입력해주세요')
      emailRef.current.focus()
      return
    }
    if (values.password === '') {
      setError('비밀번호를 입력해주세요')
      passwordRef.current.focus()
      return
    }

    setError('')
    setSubmitting(true) // 버튼을 잠근다 (두 번 누르지 않게)

    try {
      // 2. 서버에 로그인 요청
      const result = await login({ email: values.email.trim(), password: values.password })

      // 3. App 에 알려준다 (토큰과 내 정보, 그리고 어디에 저장할지)
      onLogin(result.accessToken, result.user, keep)

      // 4. 홈으로 이동
      navigate('/')
    } catch (err) {
      // 비밀번호가 틀렸거나, 서버가 꺼져 있는 경우
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

      // 비밀번호를 다시 치기 쉽게 입력한 글자 전체를 선택해 둔다
      passwordRef.current.select()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login">
      <div className="login__top">
        {/* ----- 왼쪽: 소개 ----- */}
        {/* 배경 사진은 public/images/LoginPage.jpg 다. 없으면 그라데이션만 보인다 */}
        <section className="login__hero">
          <p className="login__hand">오늘 뭐 먹지?</p>

          <h2 className="login__headline">
            맛있는 고민을
            <br />
            <strong>쉽게, 즐겁게</strong>
          </h2>

          <p className="login__lead">
            오늘도 맛있는 하루가 되도록
            <br />
            다양한 레시피와 추천을 준비했어요.
          </p>

          <ul className="login__points">
            {POINTS.map((point) => (
              <li key={point.title}>
                <span className="login__point-emoji">{point.emoji}</span>
                <strong className="login__point-title">{point.title}</strong>
                <span className="muted login__point-desc">{point.desc}</span>
              </li>
            ))}
          </ul>
        </section>

        {/* ----- 오른쪽: 로그인 카드 ----- */}
        <section className="card login__card">
          <h2 className="login__title">로그인</h2>
          <p className="muted login__subtitle">맛있는 레시피와 추천을 만나보세요!</p>

          <form onSubmit={handleSubmit}>
            <label className="login__field">
              <span className="login__icon">✉️</span>
              <input
                ref={emailRef}
                type="email"
                name="email"
                value={values.email}
                onChange={handleChange}
                placeholder="이메일을 입력해주세요"
              />
            </label>

            <label className="login__field">
              <span className="login__icon">🔒</span>
              <PasswordInput
                ref={passwordRef}
                name="password"
                value={values.password}
                onChange={handleChange}
                placeholder="비밀번호를 입력해주세요"
              />
            </label>

            <div className="login__row">
              <label className="login__keep">
                <input type="checkbox" checked={keep} onChange={(e) => setKeep(e.target.checked)} />
                로그인 상태 유지
              </label>

              <Link to="/password-reset" className="muted login__forgot">
                비밀번호를 잊으셨나요?
              </Link>
            </div>

            <ErrorMessage message={error} />

            <button type="submit" disabled={submitting} className="login__submit">
              {submitting ? '로그인 중...' : '로그인 →'}
            </button>
          </form>

          <p className="muted login__footer">
            아직 계정이 없으신가요?{' '}
            <Link to="/register" className="login__link">
              회원가입 →
            </Link>
          </p>
        </section>
      </div>

      {/* 아래: 메뉴 게임 배너. 첫 화면과 같은 부품을 쓴다.
          목록을 못 받았으면 아예 그리지 않는다 - 빈 배너가 뜨는 것보다 낫다 */}
      {games.length > 0 && <GamePromo games={games} variant="full" />}
    </div>
  )
}
