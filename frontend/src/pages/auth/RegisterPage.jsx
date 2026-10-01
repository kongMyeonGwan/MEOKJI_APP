import { useState } from 'react'
import { Link } from 'react-router-dom'
import { register } from '../../api/auth'
import AddressPicker from '../../components/common/AddressPicker'
import ErrorMessage from '../../components/common/ErrorMessage'
import FormField from '../../components/common/FormField'
import PasswordInput from '../../components/common/PasswordInput'
import useForm from '../../hooks/useForm'
import './RegisterPage.css'

// 회원가입 화면

// 성별 선택지. 서버(DB)에 저장되는 값과 같아야 한다.
const GENDERS = [
  { value: '', label: '선택 안 함' },
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'OTHER', label: '기타' },
]

// 이메일 모양 검사용 정규식: "글자@글자.글자"
const EMAIL_RULE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 입력값을 검사해서 칸별 에러 문구를 돌려준다. 에러가 없으면 빈 객체 {}
const validate = (v) => {
  const errors = {}

  if (!v.email.trim()) {
    errors.email = '이메일을 입력해주세요'
  } else if (!EMAIL_RULE.test(v.email.trim())) {
    errors.email = '이메일 형식이 아니에요 (예: you@example.com)'
  }

  if (v.password.length < 8) {
    errors.password = '비밀번호는 8자 이상이어야 합니다'
  }

  if (v.passwordConfirm !== v.password) {
    errors.passwordConfirm = '비밀번호가 일치하지 않습니다'
  }

  if (!v.nickname.trim()) {
    errors.nickname = '닉네임을 입력해주세요'
  }

  return errors
}

// 비밀번호 강도 점수 (0~4점): 조건 하나를 만족할 때마다 +1
const getStrength = (pw) => {
  let score = 0
  if (pw.length >= 8) score++ // 8자 이상
  if (/[a-zA-Z]/.test(pw)) score++ // 영문 포함
  if (/\d/.test(pw)) score++ // 숫자 포함
  if (/[^a-zA-Z\d]/.test(pw)) score++ // 특수문자 포함
  return score
}

const STRENGTH_LABELS = ['', '약함', '보통', '강함', '아주 강함']

export default function RegisterPage() {
  // 입력칸 값들 (성별은 선택 항목)
  const { values, handleChange } = useForm({
    email: '',
    password: '',
    passwordConfirm: '',
    nickname: '',
    gender: '',
  })

  // 사는 곳(선택). AddressPicker 가 두 값을 같이 바꿔서 따로 둔다
  const [sido, setSido] = useState('')
  const [sigungu, setSigungu] = useState('')

  // 사용자가 한 번이라도 건드린 칸. 예) { email: true }
  // 아직 안 건드린 칸에는 처음부터 빨간 글씨를 띄우지 않으려고 쓴다
  const [touched, setTouched] = useState({})

  const [error, setError] = useState('') // 서버가 보낸 에러 문구
  const [submitting, setSubmitting] = useState(false)
  const [doneUser, setDoneUser] = useState(null) // 가입이 끝나면 가입한 사람 정보

  // 지금 입력값으로 검사한 결과 (입력할 때마다 다시 계산된다)
  const errors = validate(values)
  const strength = getStrength(values.password)

  // 입력칸에서 빠져나오면 그 칸을 건드렸다고 기록한다
  const handleBlur = (e) => {
    setTouched((prev) => ({
      ...prev,
      [e.target.name]: true,
    }))
  }

  // 건드린 칸의 에러만 돌려준다
  const errorOf = (name) => {
    return touched[name] ? errors[name] : ''
  }

  // 주소를 골랐을 때 (AddressPicker 가 시/도, 시/군/구를 같이 알려준다)
  const changeAddress = (nextSido, nextSigungu) => {
    setSido(nextSido)
    setSigungu(nextSigungu)
  }

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    // 제출하면 모든 칸을 건드린 것으로 처리 → 숨어 있던 에러가 전부 보인다
    setTouched({ email: true, password: true, passwordConfirm: true, nickname: true })

    // 에러가 하나라도 있으면 서버에 보내지 않는다
    if (Object.keys(errors).length > 0) {
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const user = await register({
        email: values.email.trim(),
        password: values.password,
        nickname: values.nickname.trim(),
        // 선택 안 한 항목은 null 로 보낸다
        gender: values.gender || null,
        addressSido: sido || null,
        addressSigungu: sigungu || null,
      })
      setDoneUser(user)
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

      {/* noValidate: 브라우저 기본 검사 말풍선 대신 아래 에러 문구로 보여준다 */}
      <form onSubmit={handleSubmit} noValidate>
        <FormField className="register__field" label="이메일" required error={errorOf('email')}>
          <input
            type="email"
            name="email"
            value={values.email}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="you@example.com"
          />
        </FormField>

        <FormField className="register__field" label="비밀번호" required error={errorOf('password')}>
          <PasswordInput
            name="password"
            autoComplete="new-password"
            value={values.password}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="8자 이상"
          />

          {/* 비밀번호 강도 막대. 점수만큼 칸이 채워진다 */}
          {values.password && (
            <div className={`register__strength is-level-${strength}`}>
              {[1, 2, 3, 4].map((level) => (
                <span key={level} className={level <= strength ? 'is-on' : ''} />
              ))}
              <em className="muted">강도: {STRENGTH_LABELS[strength]}</em>
            </div>
          )}
        </FormField>

        <FormField className="register__field" label="비밀번호 확인" required error={errorOf('passwordConfirm')}>
          <PasswordInput
            name="passwordConfirm"
            autoComplete="new-password"
            value={values.passwordConfirm}
            onChange={handleChange}
            onBlur={handleBlur}
          />
        </FormField>

        <FormField className="register__field" label="닉네임" required error={errorOf('nickname')}>
          <input
            type="text"
            name="nickname"
            value={values.nickname}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="최대 50자"
            maxLength={50}
          />
        </FormField>

        <p className="muted register__optional">
          아래는 선택 항목이에요. 사는 곳을 골라두면 그 지역 날씨로 메뉴를 추천해드립니다.
        </p>

        <label className="register__field">
          <span>성별 (선택)</span>
          <select name="gender" value={values.gender} onChange={handleChange}>
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
