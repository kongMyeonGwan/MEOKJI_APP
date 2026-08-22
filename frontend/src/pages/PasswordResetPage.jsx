import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { changePassword, sendResetCode, verifyResetCode } from '../api/auth'
import { parseApiError } from '../utils/apiError'
import ErrorMessage from '../components/common/ErrorMessage'
import '../styles/pages/PasswordResetPage.css'

/**
 * 비밀번호 재설정 — 이메일 인증번호 방식.
 *
 * 한 화면 안에서 단계만 바꾼다. 라우트를 3개로 나누면 새로고침했을 때
 * 인증번호를 잃어버린 채 2단계 화면만 덩그러니 남는다.
 *
 * 로그인 실패 20회로 잠긴 계정도 이 절차로 풀린다.
 */

const STEP = { SEND: 'send', VERIFY: 'verify', CHANGE: 'change', DONE: 'done' }

export default function PasswordResetPage() {
  const [step, setStep] = useState(STEP.SEND)
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')

  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [notice, setNotice] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const navigate = useNavigate()

  /** 각 단계 공통 처리: 에러 초기화 -> 요청 -> 실패 시 메시지 표시. */
  const run = async (fn) => {
    setMessage('')
    setFieldErrors({})
    setSubmitting(true)
    try {
      await fn()
    } catch (err) {
      const { fieldErrors: fe, message: msg } = parseApiError(err)
      setFieldErrors(fe)
      setMessage(msg)
    } finally {
      setSubmitting(false)
    }
  }

  const onSend = (e) => {
    e.preventDefault()
    if (!email.trim()) {
      setFieldErrors({ email: '이메일을 입력해주세요' })
      return
    }
    run(async () => {
      const res = await sendResetCode(email.trim())
      setNotice(res.message)
      setStep(STEP.VERIFY)
    })
  }

  const onVerify = (e) => {
    e.preventDefault()
    if (code.length !== 6) {
      setFieldErrors({ code: '인증번호 6자리를 입력해주세요' })
      return
    }
    run(async () => {
      const res = await verifyResetCode(email.trim(), code)
      setNotice(res.message)
      setStep(STEP.CHANGE)
    })
  }

  const onChange = (e) => {
    e.preventDefault()
    const errors = {}
    if (newPassword.length < 8) errors.new_password = '비밀번호는 8자 이상이어야 합니다'
    if (newPassword !== passwordConfirm) {
      errors.passwordConfirm = '비밀번호가 일치하지 않습니다'
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }
    run(async () => {
      await changePassword(email.trim(), code, newPassword)
      setNotice('')
      setStep(STEP.DONE)
    })
  }

  /** 인증번호를 다시 받는다. 시도 횟수를 초과했을 때 필요하다. */
  const onResend = () =>
    run(async () => {
      const res = await sendResetCode(email.trim())
      setCode('')
      setNotice(res.message)
    })

  return (
    <div className="card reset">
      <h2 className="reset__title">비밀번호 재설정</h2>

      <ol className="reset__steps">
        <li className={step === STEP.SEND ? 'is-current' : ''}>이메일 확인</li>
        <li className={step === STEP.VERIFY ? 'is-current' : ''}>인증번호 입력</li>
        <li className={step === STEP.CHANGE ? 'is-current' : ''}>새 비밀번호</li>
      </ol>

      {step === STEP.SEND && (
        <form onSubmit={onSend} noValidate>
          <p className="muted reset__desc">
            가입하신 이메일로 인증번호를 보내드립니다.
            <br />
            로그인 실패로 잠긴 계정도 여기서 풀 수 있습니다.
          </p>
          <label className="reset__field">
            <span>이메일</span>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
              autoFocus
            />
            <ErrorMessage message={fieldErrors.email} />
          </label>
          <ErrorMessage message={message} />
          <button type="submit" disabled={submitting} className="reset__submit">
            {submitting ? '보내는 중...' : '인증번호 받기'}
          </button>
        </form>
      )}

      {step === STEP.VERIFY && (
        <form onSubmit={onVerify} noValidate>
          {notice && <p className="reset__notice">{notice}</p>}
          <label className="reset__field">
            <span>인증번호 6자리</span>
            <input
              type="text"
              inputMode="numeric"
              value={code}
              // 숫자만, 6자리까지. 공백이나 하이픈이 섞여 들어오는 것을 막는다.
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="000000"
              maxLength={6}
              autoFocus
            />
            <ErrorMessage message={fieldErrors.code} />
          </label>
          <ErrorMessage message={message} />
          <button type="submit" disabled={submitting} className="reset__submit">
            {submitting ? '확인 중...' : '확인'}
          </button>
          <button
            type="button"
            onClick={onResend}
            disabled={submitting}
            className="reset__ghost"
          >
            인증번호 다시 받기
          </button>
        </form>
      )}

      {step === STEP.CHANGE && (
        <form onSubmit={onChange} noValidate>
          {notice && <p className="reset__notice">{notice}</p>}
          <label className="reset__field">
            <span>새 비밀번호</span>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="8자 이상"
              autoComplete="new-password"
              autoFocus
            />
            <ErrorMessage message={fieldErrors.new_password} />
          </label>
          <label className="reset__field">
            <span>새 비밀번호 확인</span>
            <input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              autoComplete="new-password"
            />
            <ErrorMessage message={fieldErrors.passwordConfirm} />
          </label>
          <ErrorMessage message={message} />
          <button type="submit" disabled={submitting} className="reset__submit">
            {submitting ? '변경 중...' : '비밀번호 변경'}
          </button>
        </form>
      )}

      {step === STEP.DONE && (
        <>
          <p>비밀번호가 변경되었습니다.</p>
          <p className="muted">계정이 잠겨 있었다면 함께 해제되었습니다.</p>
          <button
            type="button"
            onClick={() => navigate('/login')}
            className="reset__submit"
          >
            로그인하러 가기
          </button>
        </>
      )}

      <p className="muted reset__footer">
        <Link to="/login" className="reset__link">로그인으로 돌아가기</Link>
      </p>
    </div>
  )
}
