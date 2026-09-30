import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { getErrorMessage } from '../../utils/apiError'
import ErrorMessage from '../../components/common/ErrorMessage'
import '../../styles/pages/LoginPage.css'

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
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('') // 화면에 보여줄 에러 문장
  const [submitting, setSubmitting] = useState(false) // 서버 응답을 기다리는 중인지

  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    // 1. 입력 확인
    if (email.trim() === '') {
      setError('이메일을 입력해주세요')
      return
    }
    if (password === '') {
      setError('비밀번호를 입력해주세요')
      return
    }

    setError('')
    setSubmitting(true) // 버튼을 잠근다 (두 번 누르지 않게)

    try {
      // 2. 서버에 로그인 요청
      const result = await login({ email: email.trim(), password: password })

      // 3. App 에 알려준다 (토큰과 내 정보)
      onLogin(result.access_token, result.user)

      // 4. 홈으로 이동
      navigate('/')
    } catch (err) {
      // 비밀번호가 틀렸거나, 서버가 꺼져 있는 경우
      setError(getErrorMessage(err))
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
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />
        </label>

        <label className="login__field">
          <span>비밀번호</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
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
