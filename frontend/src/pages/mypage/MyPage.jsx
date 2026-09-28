import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AddressPicker from '../../components/common/AddressPicker'
import ApiStatus from '../../components/common/ApiStatus'
import ErrorMessage from '../../components/common/ErrorMessage'
import { addBan, getBans, removeBan } from '../../api/bans'
import { changeMyPassword, updateProfile, withdraw } from '../../api/users'
import { useAuth } from '../../contexts/AuthContext'
import { useProfile } from '../../contexts/ProfileContext'
import { parseApiError } from '../../utils/apiError'
import { formatDate } from '../../utils/format'
import '../../styles/pages/MyPage.css'

// 마이페이지. 내 정보, 못 먹는 재료, 계정 관리.
//
// 못 먹는 재료는 두 종류다.
//   ALLERGY - 못 먹는 것. AI 추천에서 반드시 뺀다.
//   DISLIKE - 싫어하는 것. 가능하면 피하고 대체재료를 제안한다.
//
// 이 화면은 ProtectedRoute 안에 있어서 user 는 항상 있다.
//
// 비밀번호 변경이랑 탈퇴는 접어뒀다.
// 재료를 지우러 왔다가 '회원 탈퇴' 버튼을 마주치면 안 되기 때문이다.

// 성별 선택지. DB 의 CHECK 제약이랑 값이 같아야 한다.
const GENDERS = [
  { value: '', label: '선택 안 함' },
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'OTHER', label: '기타' },
]

export default function MyPage() {
  const { user, logout } = useAuth()
  const { profile, reload } = useProfile()
  const navigate = useNavigate()

  const [bans, setBans] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [name, setName] = useState('')
  const [banType, setBanType] = useState('DISLIKE')
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [removingId, setRemovingId] = useState(null)

  // 화면이 처음 열릴 때 내 재료 목록을 불러온다.
  // 두 번째 인자가 [] 라서 딱 한 번만 실행된다.
  useEffect(() => {
    getBans()
      .then(function (data) {
        setBans(data)
      })
      .catch(function (err) {
        setLoadError(parseApiError(err).message)
      })
      .finally(function () {
        setLoading(false)
      })
  }, [])

  // 추가 버튼을 눌렀을 때
  async function onAdd(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    const trimmed = name.trim()

    if (!trimmed) {
      setFormError('재료 이름을 입력해주세요')
      return
    }

    // DB 에도 같은 규칙이 있다.
    // 여기서 먼저 막는 건 서버까지 갔다 오는 걸 아끼려는 것이고
    // 서버도 409 로 다시 막는다.
    let already = false
    for (let i = 0; i < bans.length; i++) {
      if (bans[i].ingredient_name === trimmed) {
        already = true
        break
      }
    }

    if (already) {
      setFormError('이미 등록한 재료입니다')
      return
    }

    setSubmitting(true)

    try {
      const created = await addBan({
        ingredientName: trimmed,
        banType: banType,
      })

      // 서버가 준 것을 목록 맨 뒤에 붙인다. 등록 순으로 보여줘서 뒤가 맞다.
      setBans(bans.concat([created]))
      setName('')
      setFormError('')
    } catch (err) {
      const parsed = parseApiError(err)

      let text = parsed.fieldErrors.ingredient_name
      if (!text) {
        text = parsed.message
      }
      setFormError(text)
    } finally {
      setSubmitting(false)
    }
  }

  // ✕ 를 눌렀을 때
  async function onRemove(ban) {
    setRemovingId(ban.id) // 이 줄의 버튼만 잠근다

    try {
      await removeBan(ban.id)

      setBans(
        bans.filter(function (item) {
          return item.id !== ban.id
        }),
      )
      setFormError('')
    } catch (err) {
      // 화면에서 먼저 지우지 않는다. 실패했는데 화면에서만 사라지면
      // 새로고침할 때 되살아나서 헷갈린다.
      setFormError(parseApiError(err).message)
    } finally {
      setRemovingId(null)
    }
  }

  // 알레르기로 등록한 것만 센다
  const allergies = bans.filter(function (item) {
    return item.ban_type === 'ALLERGY'
  })

  let nickname = '-'
  let email = '-'
  let createdAt = null
  if (user) {
    nickname = user.nickname
    email = user.email
    createdAt = user.created_at
  }

  let addLabel = '추가'
  if (submitting) {
    addLabel = '추가 중…'
  }

  return (
    <div className="mypage">
      <section className="card">
        <h2 className="section-title">내 정보</h2>
        <dl className="mypage__info">
          <dt>닉네임</dt>
          <dd>{nickname}</dd>
          <dt>이메일</dt>
          <dd>{email}</dd>
          <dt>가입일</dt>
          <dd>{formatDate(createdAt)}</dd>
          {profile && (
            <>
              <dt>활동</dt>
              <dd className="muted">
                요리 {profile.cook_count}회 · 연속 {profile.streak_days}일
              </dd>
            </>
          )}
        </dl>
      </section>

      <ProfileForm profile={profile} onSaved={reload} />

      <section className="card">
        <h2 className="section-title">못 먹는 재료</h2>
        <p className="muted mypage__desc">
          여기 등록한 재료는 AI 추천과 레시피에서 제외됩니다.
          {allergies.length > 0 && (
            <>
              {' '}
              알레르기로 등록한 <strong>{allergies.length}개</strong>는 대체재료
              없이 완전히 제외합니다.
            </>
          )}
        </p>

        <form className="mypage__form" onSubmit={onAdd}>
          <input
            type="text"
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              setFormError('')
            }}
            placeholder="재료 이름 (예: 오이)"
            maxLength={100}
            aria-label="재료 이름"
            disabled={submitting}
          />
          <select
            value={banType}
            onChange={(e) => setBanType(e.target.value)}
            aria-label="종류"
            disabled={submitting}
          >
            <option value="DISLIKE">싫어함</option>
            <option value="ALLERGY">알레르기</option>
          </select>
          <button type="submit" className="btn" disabled={submitting}>
            {addLabel}
          </button>
        </form>
        <ErrorMessage message={formError} />

        {/* 목록을 못 불러온 에러는 추가/삭제 에러랑 따로 보여준다.
            한 자리에 겹쳐 쓰면 나중에 뜬 쪽이 앞의 원인을 덮는다. */}
        <ErrorMessage message={loadError} />

        {loading && <p className="empty">불러오는 중…</p>}

        {!loading && bans.length === 0 && (
          <p className="empty">등록한 재료가 없습니다.</p>
        )}

        {!loading && bans.length > 0 && (
          <ul className="mypage__bans">
            {bans.map(function (ban) {
              const isAllergy = ban.ban_type === 'ALLERGY'

              let tagClass = 'tag tag--dislike'
              let tagText = '싫어함'
              if (isAllergy) {
                tagClass = 'tag tag--allergy'
                tagText = '알레르기'
              }

              return (
                <li key={ban.id} className="mypage__ban">
                  <span className={tagClass}>{tagText}</span>
                  <span className="mypage__ban-name">{ban.ingredient_name}</span>
                  <button
                    type="button"
                    className="mypage__remove"
                    onClick={() => onRemove(ban)}
                    disabled={removingId === ban.id}
                    aria-label={ban.ingredient_name + ' 삭제'}
                  >
                    ✕
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <AccountSection
        onWithdrawn={() => {
          logout()
          navigate('/')
        }}
      />

      {/* 백엔드가 연결됐는지 확인하는 개발용 카드 */}
      <ApiStatus />
    </div>
  )
}

// 닉네임, 성별, 주소를 고치는 폼.
//
// 주소를 여기서 받는 이유는 첫 화면의 날씨 때문이다.
// 위치 권한을 거부한 사람은 좌표가 없어서 기본 좌표 날씨가 뜨는데,
// 사는 곳을 적어두면 그 지역 날씨로 추천을 받을 수 있다.
function ProfileForm({ profile, onSaved }) {
  const [nickname, setNickname] = useState('')
  const [gender, setGender] = useState('')
  const [sido, setSido] = useState('')
  const [sigungu, setSigungu] = useState('')

  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  // 서버에서 값이 도착하면 입력칸을 채운다.
  // profile 이 null 인 동안은 빈 칸으로 뒀다가 온 뒤에 한 번 맞춰준다.
  useEffect(() => {
    if (!profile) {
      return
    }

    setNickname(profile.nickname)

    if (profile.gender) {
      setGender(profile.gender)
    } else {
      setGender('')
    }

    if (profile.address_sido) {
      setSido(profile.address_sido)
    } else {
      setSido('')
    }

    if (profile.address_sigungu) {
      setSigungu(profile.address_sigungu)
    } else {
      setSigungu('')
    }
  }, [profile])

  if (!profile) {
    return null
  }

  async function onSubmit(e) {
    e.preventDefault()

    setSaving(true)
    setError('')
    setDone(false)

    // 빈 문자열은 "선택 안 함" 이다. null 로 보내야 DB 에서도 비워진다.
    let genderValue = null
    if (gender) {
      genderValue = gender
    }

    let sidoValue = null
    if (sido) {
      sidoValue = sido
    }

    let sigunguValue = null
    if (sigungu) {
      sigunguValue = sigungu
    }

    try {
      await updateProfile({
        nickname: nickname.trim(),
        gender: genderValue,
        addressSido: sidoValue,
        addressSigungu: sigunguValue,
      })
      setDone(true)
      onSaved()
    } catch (err) {
      const parsed = parseApiError(err)

      let text = parsed.fieldErrors.nickname
      if (!text) {
        text = parsed.message
      }
      setError(text)
    } finally {
      setSaving(false)
    }
  }

  // 주소를 골랐을 때. 시/도랑 시/군/구를 한 번에 바꾼다.
  // 따로 바꾸면 있지도 않은 조합이 잠깐 만들어진다.
  function onAddressChange(nextSido, nextSigungu) {
    setSido(nextSido)
    setSigungu(nextSigungu)
  }

  let saveLabel = '저장'
  if (saving) {
    saveLabel = '저장 중…'
  }

  return (
    <section className="card">
      <h2 className="section-title">정보 수정</h2>
      <p className="muted mypage__desc">
        이메일은 로그인 아이디라 바꿀 수 없습니다. 사는 곳을 골라두면 위치 권한을
        주지 않아도 그 지역 날씨로 메뉴를 추천해드려요.
      </p>

      <form className="mypage__profile" onSubmit={onSubmit}>
        <label className="mypage__field">
          <span>닉네임</span>
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            maxLength={50}
            required
          />
        </label>

        <label className="mypage__field">
          <span>성별</span>
          <select value={gender} onChange={(e) => setGender(e.target.value)}>
            {GENDERS.map(function (item) {
              return (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </label>

        <div className="mypage__field">
          <span>사는 곳</span>
          <AddressPicker
            sido={sido}
            sigungu={sigungu}
            onChange={onAddressChange}
            disabled={saving}
          />
        </div>

        <div className="mypage__field-actions">
          <button type="submit" className="btn" disabled={saving}>
            {saveLabel}
          </button>
          {done && <span className="muted">저장했습니다.</span>}
        </div>
      </form>

      <ErrorMessage message={error} />
    </section>
  )
}

// 계정 관리. 비밀번호 변경이랑 회원 탈퇴.
// 둘 다 접혀 있다. 실수로 누를 만한 자리에 두지 않으려는 것이다.
function AccountSection({ onWithdrawn }) {
  // 열려 있는 것. 'password' 나 'withdraw', 아무것도 안 열렸으면 null
  const [open, setOpen] = useState(null)

  // 같은 걸 또 누르면 닫히고 다른 걸 누르면 그쪽이 열린다
  function toggle(name) {
    if (open === name) {
      setOpen(null)
    } else {
      setOpen(name)
    }
  }

  return (
    <section className="card">
      <h2 className="section-title">계정 관리</h2>

      <div className="mypage__account-tabs">
        <button
          type="button"
          className="btn btn--ghost btn--sm"
          onClick={() => toggle('password')}
        >
          비밀번호 변경
        </button>
        <button
          type="button"
          className="btn btn--ghost btn--sm mypage__danger"
          onClick={() => toggle('withdraw')}
        >
          회원 탈퇴
        </button>
      </div>

      {open === 'password' && <PasswordForm onDone={() => setOpen(null)} />}
      {open === 'withdraw' && <WithdrawForm onWithdrawn={onWithdrawn} />}
    </section>
  )
}

// 비밀번호 변경. 지금 비밀번호를 아는 사람만 쓸 수 있다.
function PasswordForm({ onDone }) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function onSubmit(e) {
    e.preventDefault()

    // 두 번 입력받아서 오타를 거른다.
    // 바꾼 비밀번호가 오타면 다음 로그인부터 못 들어온다.
    if (next !== confirm) {
      setError('새 비밀번호가 서로 다릅니다')
      return
    }

    setSaving(true)
    setError('')

    try {
      await changeMyPassword(current, next)
      window.alert('비밀번호가 변경되었습니다.')
      onDone()
    } catch (err) {
      const parsed = parseApiError(err)

      let text = parsed.fieldErrors.new_password
      if (!text) {
        text = parsed.message
      }
      setError(text)
    } finally {
      setSaving(false)
    }
  }

  let saveLabel = '비밀번호 변경'
  if (saving) {
    saveLabel = '변경 중…'
  }

  return (
    <form className="mypage__profile" onSubmit={onSubmit}>
      <label className="mypage__field">
        <span>현재 비밀번호</span>
        <input
          type="password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
          autoComplete="current-password"
          required
        />
      </label>

      <label className="mypage__field">
        <span>새 비밀번호</span>
        <input
          type="password"
          value={next}
          onChange={(e) => setNext(e.target.value)}
          minLength={8}
          autoComplete="new-password"
          required
        />
      </label>

      <label className="mypage__field">
        <span>새 비밀번호 확인</span>
        <input
          type="password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          minLength={8}
          autoComplete="new-password"
          required
        />
      </label>

      <div className="mypage__field-actions">
        <button type="submit" className="btn" disabled={saving}>
          {saveLabel}
        </button>
        <span className="muted">8자 이상 (한글은 24자까지)</span>
      </div>

      <ErrorMessage message={error} />
    </form>
  )
}

// 회원 탈퇴.
// 행을 지우지 않고 use_yn 을 'N' 으로 바꾼다.
// 내가 쓴 글, 댓글, 좋아요, 기록도 같이 안 보이게 된다.
// 같은 이메일로 다시 가입할 수는 있다.
function WithdrawForm({ onWithdrawn }) {
  const [password, setPassword] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function onSubmit(e) {
    e.preventDefault()

    const ok = window.confirm(
      '정말 탈퇴하시겠어요?\n작성하신 글과 기록이 모두 보이지 않게 됩니다.',
    )
    if (!ok) {
      return
    }

    setSaving(true)
    setError('')

    try {
      await withdraw(password)
      window.alert('탈퇴 처리되었습니다. 그동안 이용해주셔서 감사합니다.')
      onWithdrawn()
    } catch (err) {
      setError(parseApiError(err).message)
    } finally {
      setSaving(false)
    }
  }

  let withdrawLabel = '탈퇴하기'
  if (saving) {
    withdrawLabel = '처리 중…'
  }

  return (
    <form className="mypage__profile" onSubmit={onSubmit}>
      <p className="muted mypage__desc">
        탈퇴하면 작성하신 글 · 댓글 · 식사 기록이 모두 보이지 않게 됩니다. 같은
        이메일로 다시 가입할 수는 있지만, 예전 기록은 돌아오지 않습니다.
      </p>

      <label className="mypage__field">
        <span>비밀번호 확인</span>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
      </label>

      <div className="mypage__field-actions">
        <button
          type="submit"
          className="btn btn--outline mypage__danger"
          disabled={saving}
        >
          {withdrawLabel}
        </button>
      </div>

      <ErrorMessage message={error} />
    </form>
  )
}
