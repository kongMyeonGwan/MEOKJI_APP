import { useState } from 'react'
import { Link } from 'react-router-dom'
import { register } from '../../api/auth'
import { getErrorMessage } from '../../utils/apiError'
import AddressPicker from '../../components/common/AddressPicker'
import ErrorMessage from '../../components/common/ErrorMessage'
import '../../styles/pages/RegisterPage.css'

// 회원가입 화면

// 성별 선택지. 서버(DB)에 저장되는 값과 같아야 한다.
const GENDERS = [
  { value: '', label: '선택 안 함' },
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'OTHER', label: '기타' },
]

export default function RegisterPage() {
  // 필수 항목
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [nickname, setNickname] = useState('')

  // 선택 항목
  const [gender, setGender] = useState('')
  const [sido, setSido] = useState('')
  const [sigungu, setSigungu] = useState('')

  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [doneUser, setDoneUser] = useState(null) // 가입이 끝나면 가입한 사람 정보

  // 주소를 골랐을 때 (AddressPicker 가 시/도, 시/군/구를 같이 알려준다)
  const changeAddress = (nextSido, nextSigungu) => {
    setSido(nextSido)
    setSigungu(nextSigungu)
  }

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    // 서버에 보내기 전에 먼저 검사한다
    if (!email.trim()) {
      setError('이메일을 입력해주세요')
      return
    }
    if (password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다')
      return
    }
    if (password !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다')
      return
    }
    if (!nickname.trim()) {
      setError('닉네임을 입력해주세요')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const user = await register({
        email: email.trim(),
        password: password,
        nickname: nickname.trim(),
        // 선택 안 한 항목은 null 로 보낸다
        gender: gender || null,
        addressSido: sido || null,
        addressSigungu: sigungu || null,
      })
      setDoneUser(user)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  // 가입이 끝나면 폼 대신 안내 화면을 보여준다
  if (doneUser) {
    return (
      <div className="card register">
        <h2 className="register__title">가입 완료</h2>
        <p>
          <strong>{doneUser.nickname}</strong>님, 환영합니다.
        </p>
        <p className="muted">이제 로그인하고 오늘 뭐 먹을지 찾아보세요.</p>
        <div className="register__actions">
          <Link to="/login" className="btn">
            로그인하러 가기
          </Link>
          <Link to="/" className="register__link">
            홈으로
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="card register">
      <h2 className="register__title">회원가입</h2>

      <form onSubmit={handleSubmit}>
        <label className="register__field">
          <span>이메일</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />
        </label>

        <label className="register__field">
          <span>비밀번호</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="8자 이상"
          />
        </label>

        <label className="register__field">
          <span>비밀번호 확인</span>
          <input
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
          />
        </label>

        <label className="register__field">
          <span>닉네임</span>
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="최대 50자"
            maxLength={50}
          />
        </label>

        <p className="muted register__optional">
          아래는 선택 항목이에요. 사는 곳을 골라두면 그 지역 날씨로 메뉴를 추천해드립니다.
        </p>

        <label className="register__field">
          <span>성별 (선택)</span>
          <select value={gender} onChange={(e) => setGender(e.target.value)}>
            {GENDERS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>

        <div className="register__field">
          <span>사는 곳 (선택)</span>
          <AddressPicker sido={sido} sigungu={sigungu} onChange={changeAddress} />
        </div>

        <ErrorMessage message={error} />

        <button type="submit" disabled={submitting} className="register__submit">
          {submitting ? '처리 중...' : '가입하기'}
        </button>
      </form>

      <p className="muted register__footer">
        이미 계정이 있으신가요?{' '}
        <Link to="/login" className="register__link">
          로그인
        </Link>
      </p>
    </div>
  )
}
