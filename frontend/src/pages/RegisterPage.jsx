import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import { parseApiError } from '../utils/apiError'
import ErrorMessage from '../components/common/ErrorMessage'
import './RegisterPage.css'

const INITIAL = { email: '', password: '', passwordConfirm: '', nickname: '' }

export default function RegisterPage() {
  const [form, setForm] = useState(INITIAL)
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(null)
  const navigate = useNavigate()

  const onChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
    // 입력을 고치는 순간 해당 칸의 에러는 지운다
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }))
  }

  /** 서버에 보내기 전 프론트에서 먼저 거를 수 있는 것들. */
  const validate = () => {
    const errors = {}
    if (!form.email.trim()) errors.email = '이메일을 입력해주세요'
    if (form.password.length < 8) errors.password = '비밀번호는 8자 이상이어야 합니다'
    if (form.password !== form.passwordConfirm) {
      errors.passwordConfirm = '비밀번호가 일치하지 않습니다'
    }
    if (!form.nickname.trim()) errors.nickname = '닉네임을 입력해주세요'
    return errors
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setMessage('')

    const errors = validate()
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSubmitting(true)
    try {
      const user = await register({
        email: form.email.trim(),
        password: form.password,
        nickname: form.nickname.trim(),
      })
      setDone(user)
    } catch (err) {
      const { fieldErrors: fe, message: msg } = parseApiError(err)
      setFieldErrors(fe)
      setMessage(msg)
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <div className="card register">
        <h2 className="register__title">가입 완료</h2>
        <p>
          <strong>{done.nickname}</strong>님, 환영합니다.
        </p>
        <p className="muted">이제 로그인하고 오늘 뭐 먹을지 찾아보세요.</p>
        <div className="register__actions">
          <button type="button" onClick={() => navigate('/login')}>
            로그인하러 가기
          </button>
          <Link to="/" className="register__link">홈으로</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="card register">
      <h2 className="register__title">회원가입</h2>

      <form onSubmit={onSubmit} noValidate>
        <label className="register__field">
          <span>이메일</span>
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={onChange}
            placeholder="you@example.com"
            autoComplete="email"
          />
          <ErrorMessage message={fieldErrors.email} />
        </label>

        <label className="register__field">
          <span>비밀번호</span>
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={onChange}
            placeholder="8자 이상"
            autoComplete="new-password"
          />
          <ErrorMessage message={fieldErrors.password} />
        </label>

        <label className="register__field">
          <span>비밀번호 확인</span>
          <input
            type="password"
            name="passwordConfirm"
            value={form.passwordConfirm}
            onChange={onChange}
            autoComplete="new-password"
          />
          <ErrorMessage message={fieldErrors.passwordConfirm} />
        </label>

        <label className="register__field">
          <span>닉네임</span>
          <input
            type="text"
            name="nickname"
            value={form.nickname}
            onChange={onChange}
            placeholder="최대 50자"
            maxLength={50}
          />
          <ErrorMessage message={fieldErrors.nickname} />
        </label>

        <ErrorMessage message={message} />

        <button type="submit" disabled={submitting} className="register__submit">
          {submitting ? '처리 중...' : '가입하기'}
        </button>
      </form>

      <p className="muted register__footer">
        이미 계정이 있으신가요? <Link to="/login" className="register__link">로그인</Link>
      </p>
    </div>
  )
}
