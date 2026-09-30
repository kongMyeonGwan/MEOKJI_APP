import { useState } from 'react'
import { Link } from 'react-router-dom'
import { changePassword, sendResetCode, verifyResetCode } from '../../api/auth'
import { getErrorMessage } from '../../utils/apiError'
import ErrorMessage from '../../components/common/ErrorMessage'
import '../../styles/pages/PasswordResetPage.css'

// 비밀번호 재설정. 한 화면 안에서 단계(step)만 바꾼다.
//   1: 이메일 입력 → 인증번호 받기
//   2: 인증번호 6자리 확인
//   3: 새 비밀번호 입력
//   4: 완료
export default function PasswordResetPage() {
  const [step, setStep] = useState(1)
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')

  const [error, setError] = useState('')
  const [notice, setNotice] = useState('') // 서버가 보내준 안내 문구
  const [submitting, setSubmitting] = useState(false)

  // 1단계: 이메일로 인증번호 받기
  const handleSend = async (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    if (!email.trim()) {
      setError('이메일을 입력해주세요')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const result = await sendResetCode(email.trim())
      setNotice(result.message)
      setStep(2)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  // 2단계: 인증번호 확인
  const handleVerify = async (e) => {
    e.preventDefault()

    if (code.length !== 6) {
      setError('인증번호 6자리를 입력해주세요')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const result = await verifyResetCode(email.trim(), code)
      setNotice(result.message)
      setStep(3)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  // 인증번호 다시 받기
  const handleResend = async () => {
    setError('')
    setSubmitting(true)
    try {
      const result = await sendResetCode(email.trim())
      setCode('')
      setNotice(result.message)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  // 3단계: 새 비밀번호로 바꾸기
  const handleChange = async (e) => {
    e.preventDefault()

    if (newPassword.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다')
      return
    }
    if (newPassword !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      await changePassword(email.trim(), code, newPassword)
      setStep(4)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="card reset">
      <h2 className="reset__title">비밀번호 재설정</h2>

      <ol className="reset__steps">
        <li className={step === 1 ? 'is-current' : ''}>이메일 확인</li>
        <li className={step === 2 ? 'is-current' : ''}>인증번호 입력</li>
        <li className={step === 3 ? 'is-current' : ''}>새 비밀번호</li>
      </ol>

      {step === 1 && (
        <form onSubmit={handleSend}>
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
            />
          </label>
          <ErrorMessage message={error} />
          <button type="submit" disabled={submitting} className="reset__submit">
            {submitting ? '보내는 중...' : '인증번호 받기'}
          </button>
        </form>
      )}

      {step === 2 && (
        <form onSubmit={handleVerify}>
          {notice && <p className="reset__notice">{notice}</p>}
          <label className="reset__field">
            <span>인증번호 6자리</span>
            <input
              type="text"
              inputMode="numeric"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="000000"
              maxLength={6}
            />
          </label>
          <ErrorMessage message={error} />
          <button type="submit" disabled={submitting} className="reset__submit">
            {submitting ? '확인 중...' : '확인'}
          </button>
          <button type="button" onClick={handleResend} disabled={submitting} className="reset__ghost">
            인증번호 다시 받기
          </button>
        </form>
      )}

      {step === 3 && (
        <form onSubmit={handleChange}>
          {notice && <p className="reset__notice">{notice}</p>}
          <label className="reset__field">
            <span>새 비밀번호</span>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="8자 이상"
            />
          </label>
          <label className="reset__field">
            <span>새 비밀번호 확인</span>
            <input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
            />
          </label>
          <ErrorMessage message={error} />
          <button type="submit" disabled={submitting} className="reset__submit">
            {submitting ? '변경 중...' : '비밀번호 변경'}
          </button>
        </form>
      )}

      {step === 4 && (
        <>
          <p>비밀번호가 변경되었습니다.</p>
          <p className="muted">계정이 잠겨 있었다면 함께 해제되었습니다.</p>
          <Link to="/login" className="btn reset__submit">
            로그인하러 가기
          </Link>
        </>
      )}

      <p className="muted reset__footer">
        <Link to="/login" className="reset__link">
          로그인으로 돌아가기
        </Link>
      </p>
    </div>
  )
}
