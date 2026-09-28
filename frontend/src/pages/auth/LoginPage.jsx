import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { parseApiError } from '../../utils/apiError'
import ErrorMessage from '../../components/common/ErrorMessage'
import '../../styles/pages/LoginPage.css'

// 로그인 화면
export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // ProtectedRoute 가 넘겨준 "원래 가려던 곳". 없으면 홈으로 보낸다.
  let redirectTo = '/'
  if (location.state && location.state.from) {
    redirectTo = location.state.from
  }

  // 입력칸을 고치면 그 칸의 빨간 글씨를 지운다
  function clearError(name) {
    const next = Object.assign({}, fieldErrors)
    next[name] = undefined
    setFieldErrors(next)
    setMessage('')
  }

  // 로그인 버튼을 눌렀을 때
  async function onSubmit(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    setMessage('')

    // 로그인은 형식 검사를 최소한만 한다.
    // "8자 이상" 같은 규칙을 걸면 저장된 비밀번호 형식을 알려주는 꼴이 된다.
    const errors = {}
    if (!email.trim()) {
      errors.email = '이메일을 입력해주세요'
    }
    if (!password) {
      errors.password = '비밀번호를 입력해주세요'
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSubmitting(true)

    try {
      await login(email.trim(), password)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      const parsed = parseApiError(err)
      setFieldErrors(parsed.fieldErrors)
      setMessage(parsed.message)
    } finally {
      setSubmitting(false)
    }
  }

  let submitLabel = '로그인'
  if (submitting) {
    submitLabel = '로그인 중...'
  }

  return (
    <div className="card login">
      <h2 className="login__title">로그인</h2>

      <form onSubmit={onSubmit} noValidate>
        <label className="login__field">
          <span>이메일</span>
          <input
            type="email"
            name="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              clearError('email')
            }}
            placeholder="you@example.com"
            autoComplete="email"
            autoFocus
          />
          <ErrorMessage message={fieldErrors.email} />
        </label>

        <label className="login__field">
          <span>비밀번호</span>
          <input
            type="password"
            name="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              clearError('password')
            }}
            autoComplete="current-password"
          />
          <ErrorMessage message={fieldErrors.password} />
        </label>

        <ErrorMessage message={message} />

        <button type="submit" disabled={submitting} className="login__submit">
          {submitLabel}
        </button>
      </form>

      <p className="muted login__footer">
        <Link to="/password-reset" className="login__link">
          비밀번호를 잊으셨나요?
        </Link>
      </p>

      <p className="muted login__footer">
        아직 계정이 없으신가요?{' '}
        <Link to="/register" className="login__link">
          회원가입
        </Link>
      </p>
    </div>
  )
}
