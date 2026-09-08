import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { parseApiError } from '../utils/apiError'
import ErrorMessage from '../components/common/ErrorMessage'
import '../styles/pages/LoginPage.css'

const INITIAL = { email: '', password: '' }

export default function LoginPage() {
  const [form, setForm] = useState(INITIAL)
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // ProtectedRoute가 넘겨준 "원래 가려던 곳". 없으면 홈으로 보낸다.
  const redirectTo = location.state?.from ?? '/'

  /**
   * 입력칸에 글자를 칠 때마다 실행된다.
   *
   * 칸이 여러 개인데 함수는 하나다. e.target.name 으로 어느 칸인지 알아낸다.
   *
   *   { ...prev, [name]: value }
   *     ...prev    기존 값을 전부 복사하고
   *     [name]     그중 name 에 해당하는 칸만
   *     : value    새 값으로 덮어쓴다
   *
   *   name 이 'email' 이면 { email: '새값', password: '기존값' } 이 된다.
   *   대괄호를 씌우면 "변수에 담긴 이름"을 키로 쓴다는 뜻이다.
   *   대괄호 없이 { name: value } 라고 쓰면 진짜 'name' 이라는 칸이 만들어진다.
   *
   * 기존 값을 복사하는 이유: 리액트는 state 를 직접 고치면 안 되고
   * 항상 새 객체를 만들어 넣어줘야 화면이 다시 그려진다.
   */
  function onChange(e) {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
    // 입력을 고치는 순간 그 칸의 빨간 에러 글씨는 지운다
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }))
    setMessage('')
  }

  /** 로그인 버튼을 눌렀을 때. */
  async function onSubmit(e) {
    e.preventDefault() // 폼 기본 동작(새로고침)을 막는다
    setMessage('')

    // 로그인은 형식 검증을 최소한만 한다.
    // "8자 이상" 같은 규칙을 걸면 저장된 비밀번호의 형식을 알려주는 셈이 된다.
    const errors = {}
    if (!form.email.trim()) errors.email = '이메일을 입력해주세요'
    if (!form.password) errors.password = '비밀번호를 입력해주세요'
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSubmitting(true)
    try {
      await login(form.email.trim(), form.password)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      const { fieldErrors: fe, message: msg } = parseApiError(err)
      setFieldErrors(fe)
      setMessage(msg)
    } finally {
      setSubmitting(false)
    }
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
            value={form.email}
            onChange={onChange}
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
            value={form.password}
            onChange={onChange}
            autoComplete="current-password"
          />
          <ErrorMessage message={fieldErrors.password} />
        </label>

        <ErrorMessage message={message} />

        <button type="submit" disabled={submitting} className="login__submit">
          {submitting ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <p className="muted login__footer">
        <Link to="/password-reset" className="login__link">비밀번호를 잊으셨나요?</Link>
      </p>

      <p className="muted login__footer">
        아직 계정이 없으신가요? <Link to="/register" className="login__link">회원가입</Link>
      </p>
    </div>
  )
}
