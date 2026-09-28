import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../../api/auth'
import { parseApiError } from '../../utils/apiError'
import AddressPicker from '../../components/common/AddressPicker'
import ErrorMessage from '../../components/common/ErrorMessage'
import '../../styles/pages/RegisterPage.css'

// 회원가입 화면

// 성별 선택지. DB 의 CHECK 제약이랑 값이 같아야 한다.
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

  // 선택 항목. 안 채워도 가입된다.
  const [gender, setGender] = useState('')
  const [addressSido, setAddressSido] = useState('')
  const [addressSigungu, setAddressSigungu] = useState('')

  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(null)

  const navigate = useNavigate()

  // 입력칸을 고치면 그 칸의 빨간 글씨를 지운다
  function clearError(name) {
    const next = Object.assign({}, fieldErrors)
    next[name] = undefined
    setFieldErrors(next)
  }

  // 주소를 골랐을 때. 시/도랑 시/군/구를 한 번에 바꾼다.
  // 따로 바꾸면 시/도를 바꾼 직후에 "부산광역시 강남구" 같은
  // 있지도 않은 조합이 잠깐 만들어진다.
  function setAddress(nextSido, nextSigungu) {
    setAddressSido(nextSido)
    setAddressSigungu(nextSigungu)
  }

  // 서버에 보내기 전에 화면에서 먼저 거를 수 있는 것들을 확인한다.
  // 문제가 있으면 { 칸이름: 에러메시지 } 를 돌려준다.
  function validate() {
    const errors = {}

    if (!email.trim()) {
      errors.email = '이메일을 입력해주세요'
    }
    if (password.length < 8) {
      errors.password = '비밀번호는 8자 이상이어야 합니다'
    }
    if (password !== passwordConfirm) {
      errors.passwordConfirm = '비밀번호가 일치하지 않습니다'
    }
    if (!nickname.trim()) {
      errors.nickname = '닉네임을 입력해주세요'
    }

    return errors
  }

  // 가입 버튼을 눌렀을 때
  async function onSubmit(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    setMessage('')

    const errors = validate()
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSubmitting(true)

    // 빈 문자열은 "선택 안 함" 이다.
    // null 로 보내야 DB 에도 NULL 로 들어간다.
    let genderValue = null
    if (gender) {
      genderValue = gender
    }

    let sidoValue = null
    if (addressSido.trim()) {
      sidoValue = addressSido.trim()
    }

    let sigunguValue = null
    if (addressSigungu.trim()) {
      sigunguValue = addressSigungu.trim()
    }

    try {
      const user = await register({
        email: email.trim(),
        password: password,
        nickname: nickname.trim(),
        gender: genderValue,
        addressSido: sidoValue,
        addressSigungu: sigunguValue,
      })
      setDone(user)
    } catch (err) {
      const parsed = parseApiError(err)
      setFieldErrors(parsed.fieldErrors)
      setMessage(parsed.message)
    } finally {
      setSubmitting(false)
    }
  }

  // 가입이 끝나면 폼 대신 안내 화면을 보여준다
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
          <Link to="/" className="register__link">
            홈으로
          </Link>
        </div>
      </div>
    )
  }

  let submitLabel = '가입하기'
  if (submitting) {
    submitLabel = '처리 중...'
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
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              clearError('email')
            }}
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
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              clearError('password')
            }}
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
            value={passwordConfirm}
            onChange={(e) => {
              setPasswordConfirm(e.target.value)
              clearError('passwordConfirm')
            }}
            autoComplete="new-password"
          />
          <ErrorMessage message={fieldErrors.passwordConfirm} />
        </label>

        <label className="register__field">
          <span>닉네임</span>
          <input
            type="text"
            name="nickname"
            value={nickname}
            onChange={(e) => {
              setNickname(e.target.value)
              clearError('nickname')
            }}
            placeholder="최대 50자"
            maxLength={50}
          />
          <ErrorMessage message={fieldErrors.nickname} />
        </label>

        {/* 여기부터는 선택 항목이다.
            필수처럼 보이면 가입 도중에 그만두는 사람이 생긴다. */}
        <p className="muted register__optional">
          아래는 선택 항목이에요. 사는 곳을 골라두면 위치 권한을 주지 않아도
          그 지역 날씨로 메뉴를 추천해드립니다.
        </p>

        <label className="register__field">
          <span>성별 (선택)</span>
          <select
            name="gender"
            value={gender}
            onChange={(e) => setGender(e.target.value)}
          >
            {GENDERS.map(function (item) {
              return (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </label>

        <div className="register__field">
          <span>사는 곳 (선택)</span>
          <AddressPicker
            sido={addressSido}
            sigungu={addressSigungu}
            onChange={setAddress}
          />
        </div>

        <ErrorMessage message={message} />

        <button
          type="submit"
          disabled={submitting}
          className="register__submit"
        >
          {submitLabel}
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
