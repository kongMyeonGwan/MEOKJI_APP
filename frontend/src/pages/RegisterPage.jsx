import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import { parseApiError } from '../utils/apiError'
import ErrorMessage from '../components/common/ErrorMessage'
import '../styles/pages/RegisterPage.css'

const INITIAL = {
  email: '',
  password: '',
  passwordConfirm: '',
  nickname: '',
  // 아래 셋은 선택 항목이다. 안 채워도 가입된다.
  gender: '',
  addressSido: '',
  addressSigungu: '',
}

/** 성별 선택지. DDL의 ck_users_gender CHECK 제약과 값이 같아야 한다 */
const GENDERS = [
  { value: '', label: '선택 안 함' },
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'OTHER', label: '기타' },
]

export default function RegisterPage() {
  const [form, setForm] = useState(INITIAL)
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(null)
  const navigate = useNavigate()

  /**
   * 입력칸에 글자를 칠 때마다 실행된다.
   *
   * 칸이 여러 개인데 함수는 하나다. e.target.name 으로 어느 칸인지 알아낸다.
   *
   *   { ...prev, [name]: value }
   *     ...prev    기존 값을 전부 복사하고
   *     [name]     그중 name 에 해당하는 칸만
   *     : value    새 값으로 덮어쓴다
   *
   *   name 이 'email' 이면 { email: '새값', password: '기존값' } 이 된다.
   *   대괄호를 씌우면 "변수에 담긴 이름"을 키로 쓴다는 뜻이다.
   *   대괄호 없이 { name: value } 라고 쓰면 진짜 'name' 이라는 칸이 만들어진다.
   *
   * 기존 값을 복사하는 이유: 리액트는 state 를 직접 고치면 안 되고
   * 항상 새 객체를 만들어 넣어줘야 화면이 다시 그려진다.
   */
  function onChange(e) {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
    // 입력을 고치는 순간 그 칸의 빨간 에러 글씨는 지운다
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }))
  }

  /** 서버에 보내기 전 프론트에서 먼저 거를 수 있는 것들. */
  /**
   * 서버에 보내기 전에 화면에서 먼저 거를 수 있는 것들을 확인한다.
   *
   * 문제가 있으면 { 칸이름: 에러메시지 } 를, 없으면 빈 객체를 돌려준다.
   */
  function validate() {
    const errors = {}
    if (!form.email.trim()) errors.email = '이메일을 입력해주세요'
    if (form.password.length < 8) errors.password = '비밀번호는 8자 이상이어야 합니다'
    if (form.password !== form.passwordConfirm) {
      errors.passwordConfirm = '비밀번호가 일치하지 않습니다'
    }
    if (!form.nickname.trim()) errors.nickname = '닉네임을 입력해주세요'
    return errors
  }

  /** 가입 버튼을 눌렀을 때. */
  async function onSubmit(e) {
    e.preventDefault() // 폼 기본 동작(새로고침)을 막는다
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
        // 빈 문자열은 "선택 안 함"이다. null 로 보내야 DB 에도 NULL 로 들어간다.
        gender: form.gender || null,
        addressSido: form.addressSido.trim() || null,
        addressSigungu: form.addressSigungu.trim() || null,
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

        {/* 여기부터는 선택 항목이다. 필수처럼 보이면 가입 도중에 그만두는
            사람이 생기므로 "선택" 이라고 분명히 적어둔다. */}
        <p className="muted register__optional">
          아래는 선택 항목이에요. 사는 곳을 적어두면 그 지역 날씨로 메뉴를
          추천해드립니다.
        </p>

        <label className="register__field">
          <span>성별 (선택)</span>
          <select name="gender" value={form.gender} onChange={onChange}>
            {GENDERS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>

        <label className="register__field">
          <span>시 · 도 (선택)</span>
          <input
            type="text"
            name="addressSido"
            value={form.addressSido}
            onChange={onChange}
            placeholder="서울특별시"
            maxLength={30}
          />
        </label>

        <label className="register__field">
          <span>시 · 군 · 구 (선택)</span>
          <input
            type="text"
            name="addressSigungu"
            value={form.addressSigungu}
            onChange={onChange}
            placeholder="강남구"
            maxLength={30}
          />
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
