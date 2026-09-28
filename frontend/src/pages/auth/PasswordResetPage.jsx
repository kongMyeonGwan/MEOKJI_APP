import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { changePassword, sendResetCode, verifyResetCode } from '../../api/auth'
import { parseApiError } from '../../utils/apiError'
import ErrorMessage from '../../components/common/ErrorMessage'
import '../../styles/pages/PasswordResetPage.css'

// 비밀번호 재설정. 이메일로 인증번호를 받는 방식이다.
//
// 한 화면 안에서 단계만 바꾼다. 주소를 3개로 나누면 새로고침했을 때
// 인증번호를 잃어버린 채로 2단계 화면만 남는다.
//
// 로그인 20번 실패해서 잠긴 계정도 이걸로 풀린다.

// 화면이 네 단계로 바뀐다.
// 문자열을 직접 쓰면 오타를 내도 에러가 안 나서 여기 모아두고 꺼내 쓴다.
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

  // 인증번호를 다시 받기까지 남은 초. 0이면 지금 받을 수 있다.
  //
  // 서버가 Retry-After 헤더로 알려준다. 문구에도 초가 들어 있지만, 문구는
  // "안 된다고 말하는" 것이고 이 값은 "버튼을 못 누르게 하는" 것이다.
  // 누를 수 있게 두면 사용자는 10초마다 눌러보게 된다.
  const [cooldown, setCooldown] = useState(0)

  const navigate = useNavigate()

  // 1초마다 줄인다. 0이 되면 타이머를 안 건다.
  useEffect(() => {
    if (cooldown <= 0) {
      return
    }

    const timer = setTimeout(function () {
      setCooldown(function (left) {
        return left - 1
      })
    }, 1000)

    return function () {
      clearTimeout(timer)
    }
  }, [cooldown])

  // 요청을 보내기 전에 이전 에러를 지우고 버튼을 잠근다
  function startSubmit() {
    setMessage('')
    setFieldErrors({})
    setSubmitting(true)
  }

  // 요청이 실패했을 때 에러를 화면에 보여준다
  function showError(err) {
    const parsed = parseApiError(err)
    setFieldErrors(parsed.fieldErrors)
    setMessage(parsed.message)

    // 429 면 남은 시간만큼 버튼을 잠근다
    if (err && err.retryAfter) {
      setCooldown(err.retryAfter)
    }
  }

  // 1단계. 이메일로 인증번호 받기
  async function onSend(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    if (!email.trim()) {
      setFieldErrors({ email: '이메일을 입력해주세요' })
      return
    }

    startSubmit()
    try {
      const res = await sendResetCode(email.trim())
      setNotice(res.message)
      setStep(STEP.VERIFY) // 다음 단계로 넘어간다
    } catch (err) {
      showError(err)
    } finally {
      // 성공하든 실패하든 버튼 잠금은 풀어야 한다
      setSubmitting(false)
    }
  }

  // 2단계. 인증번호 확인
  async function onVerify(e) {
    e.preventDefault()

    if (code.length !== 6) {
      setFieldErrors({ code: '인증번호 6자리를 입력해주세요' })
      return
    }

    startSubmit()
    try {
      const res = await verifyResetCode(email.trim(), code)
      setNotice(res.message)
      setStep(STEP.CHANGE)
    } catch (err) {
      showError(err)
    } finally {
      setSubmitting(false)
    }
  }

  // 3단계. 새 비밀번호로 바꾸기
  async function onChange(e) {
    e.preventDefault()

    // 서버에 보내기 전에 화면에서 먼저 확인할 수 있는 것들
    const errors = {}
    if (newPassword.length < 8) {
      errors.new_password = '비밀번호는 8자 이상이어야 합니다'
    }
    if (newPassword !== passwordConfirm) {
      errors.passwordConfirm = '비밀번호가 일치하지 않습니다'
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    startSubmit()
    try {
      await changePassword(email.trim(), code, newPassword)
      setNotice('')
      setStep(STEP.DONE)
    } catch (err) {
      showError(err)
    } finally {
      setSubmitting(false)
    }
  }

  // 인증번호를 다시 받는다. 시도 횟수를 넘겼을 때 필요하다.
  async function onResend() {
    startSubmit()
    try {
      const res = await sendResetCode(email.trim())
      setCode('') // 입력칸을 비워서 새 번호를 넣게 한다
      setNotice(res.message)
    } catch (err) {
      showError(err)
    } finally {
      setSubmitting(false)
    }
  }

  // 지금 어느 단계인지에 따라 목록에 표시를 준다
  function stepClass(name) {
    if (step === name) {
      return 'is-current'
    }
    return ''
  }

  // 1단계 버튼 글자
  let sendLabel = '인증번호 받기'
  if (cooldown > 0) {
    sendLabel = cooldown + '초 후에 다시 받기'
  }
  if (submitting) {
    sendLabel = '보내는 중...'
  }

  let verifyLabel = '확인'
  if (submitting) {
    verifyLabel = '확인 중...'
  }

  let changeLabel = '비밀번호 변경'
  if (submitting) {
    changeLabel = '변경 중...'
  }

  return (
    <div className="card reset">
      <h2 className="reset__title">비밀번호 재설정</h2>

      <ol className="reset__steps">
        <li className={stepClass(STEP.SEND)}>이메일 확인</li>
        <li className={stepClass(STEP.VERIFY)}>인증번호 입력</li>
        <li className={stepClass(STEP.CHANGE)}>새 비밀번호</li>
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
          {/* 기다리는 동안에는 못 누른다. 문구로 말리는 것보다 확실하다. */}
          <button
            type="submit"
            disabled={submitting || cooldown > 0}
            className="reset__submit"
          >
            {sendLabel}
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
              // 숫자만 6자리까지 받는다. 공백이나 하이픈이 섞이는 걸 막는다.
              onChange={(e) => {
                const onlyNumbers = e.target.value.replace(/[^0-9]/g, '')
                setCode(onlyNumbers.slice(0, 6))
              }}
              placeholder="000000"
              maxLength={6}
              autoFocus
            />
            <ErrorMessage message={fieldErrors.code} />
          </label>
          <ErrorMessage message={message} />
          <button type="submit" disabled={submitting} className="reset__submit">
            {verifyLabel}
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
            {changeLabel}
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
        <Link to="/login" className="reset__link">
          로그인으로 돌아가기
        </Link>
      </p>
    </div>
  )
}
