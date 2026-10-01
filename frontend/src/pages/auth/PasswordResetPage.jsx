import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { changePassword, sendResetCode, verifyResetCode } from '../../api/auth'
import CodeInput from '../../components/auth/CodeInput'
import ErrorMessage from '../../components/common/ErrorMessage'
import PasswordInput from '../../components/common/PasswordInput'
import useForm from '../../hook/useForm'
import '../../styles/pages/PasswordResetPage.css'

// 비밀번호 재설정. 한 화면 안에서 단계(step)만 바꾼다.
//   1: 이메일 입력 → 인증번호 받기
//   2: 인증번호 6자리 확인
//   3: 새 비밀번호 입력
//   4: 완료

const CODE_LENGTH = 6 // 인증번호 자릿수

export default function PasswordResetPage() {
  const [step, setStep] = useState(1)
  const { values, handleChange } = useForm({ email: '', newPassword: '', passwordConfirm: '' })

  // 인증번호는 칸마다 한 글자씩 배열로 들고 있다. 예) ['1', '2', '3', '', '', '']
  const [digits, setDigits] = useState(Array(CODE_LENGTH).fill(''))
  const code = digits.join('') // 서버에 보낼 때는 이어 붙인 글자로
  const codeRef = useRef(null) // CodeInput 의 칸에 커서를 넣을 때 쓴다

  const [error, setError] = useState('')
  const [notice, setNotice] = useState('') // 서버가 보내준 안내 문구
  const [submitting, setSubmitting] = useState(false)

  // 1단계: 이메일로 인증번호 받기
  const handleSend = async (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    if (!values.email.trim()) {
      setError('이메일을 입력해주세요')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const result = await sendResetCode(values.email.trim())
      setNotice(result.message)
      setStep(2)
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
      setSubmitting(false)
    }
  }

  // 2단계: 인증번호 확인
  const handleVerify = async (e) => {
    e.preventDefault()

    // 빈 칸이 있으면 그 칸으로 커서를 옮긴다
    const emptyIndex = digits.findIndex((d) => d === '') // 첫 번째 빈 칸 번호 (없으면 -1)
    if (emptyIndex !== -1) {
      setError(`${emptyIndex + 1}번째 칸이 비었어요`)
      codeRef.current.focus(emptyIndex)
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const result = await verifyResetCode(values.email.trim(), code)
      setNotice(result.message)
      setStep(3)
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

      // 틀렸으면 전부 지우고 첫 칸부터 다시 입력하게 한다
      setDigits(Array(CODE_LENGTH).fill(''))
      codeRef.current.focus(0)
    } finally {
      setSubmitting(false)
    }
  }

  // 인증번호 다시 받기
  const handleResend = async () => {
    setError('')
    setSubmitting(true)
    try {
      const result = await sendResetCode(values.email.trim())
      setDigits(Array(CODE_LENGTH).fill('')) // 전부 지우고
      codeRef.current.focus(0) // 첫 칸으로
      setNotice(result.message)
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
      setSubmitting(false)
    }
  }

  // 3단계: 새 비밀번호로 바꾸기
  const handleChangePassword = async (e) => {
    e.preventDefault()

    if (values.newPassword.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다')
      return
    }
    if (values.newPassword !== values.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      await changePassword(values.email.trim(), code, values.newPassword)
      setStep(4)
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
              name="email"
              value={values.email}
              onChange={handleChange}
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
          <div className="reset__field">
            <span>인증번호 6자리</span>
            <CodeInput ref={codeRef} digits={digits} onChange={setDigits} />
          </div>
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
        <form onSubmit={handleChangePassword}>
          {notice && <p className="reset__notice">{notice}</p>}
          <label className="reset__field">
            <span>새 비밀번호</span>
            <PasswordInput
              name="newPassword"
              autoComplete="new-password"
              value={values.newPassword}
              onChange={handleChange}
              placeholder="8자 이상"
            />
          </label>
          <label className="reset__field">
            <span>새 비밀번호 확인</span>
            <PasswordInput
              name="passwordConfirm"
              autoComplete="new-password"
              value={values.passwordConfirm}
              onChange={handleChange}
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
