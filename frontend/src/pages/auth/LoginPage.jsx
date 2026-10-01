import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import ErrorMessage from '../../components/common/ErrorMessage'
import PasswordInput from '../../components/common/PasswordInput'
import useForm from '../../hooks/useForm'
import './LoginPage.css'

// 로그인 화면.
//
// 로그인 순서
//   1. 이메일, 비밀번호를 입력했는지 확인한다
//   2. 서버에 보낸다 → 서버가 { access_token: '토큰', user: { 내 정보 } } 를 준다
//   3. onLogin 으로 App 에 알려준다 → App 이 브라우저에 저장하고 화면을 바꾼다
//   4. 홈으로 이동한다
//
// onLogin 은 App.jsx 가 props 로 넘겨준 함수다.
export default function LoginPage({ onLogin }) {
  const { values, handleChange } = useForm({ email: '', password: '' })
  const [error, setError] = useState('') // 화면에 보여줄 에러 문장
  const [submitting, setSubmitting] = useState(false) // 서버 응답을 기다리는 중인지

  const emailRef = useRef(null)
  const passwordRef = useRef(null)

  const navigate = useNavigate()

  // 화면이 열리면 이메일 칸에 커서
  useEffect(() => {
    emailRef.current.focus()
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

      // 3. App 에 알려준다 (토큰과 내 정보)
      onLogin(result.access_token, result.user)

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
    <div className="card login">
      <h2 className="login__title">로그인</h2>

      <form onSubmit={handleSubmit}>
        <label className="login__field">
          <span>이메일</span>
          <input
            ref={emailRef}
            type="email"
            name="email"
            value={values.email}
            onChange={handleChange}
            placeholder="you@example.com"
          />
        </label>

        <label className="login__field">
          <span>비밀번호</span>
          <PasswordInput ref={passwordRef} name="password" value={values.password} onChange={handleChange} />
        </label>

        <ErrorMessage message={error} />

        <button type="submit" disabled={submitting} className="login__submit">
          {submitting ? '로그인 중...' : '로그인'}
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
