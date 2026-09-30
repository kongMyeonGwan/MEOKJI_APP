import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ApiStatus from '../../components/common/ApiStatus'
import ErrorMessage from '../../components/common/ErrorMessage'
import ProfileForm from '../../components/mypage/ProfileForm'
import AccountSection from '../../components/mypage/AccountSection'
import { addBan, getBans, removeBan } from '../../api/bans'
import { getProfile } from '../../api/users'
import { formatDate } from '../../utils/format'
import '../../styles/pages/MyPage.css'

// 마이페이지. 위에서부터 이렇게 쌓인다.
//   1. 내 정보 (이 파일)
//   2. 정보 수정 폼     → components/mypage/ProfileForm.jsx
//   3. 못 먹는 재료 (이 파일)
//   4. 비밀번호 · 탈퇴  → components/mypage/AccountSection.jsx
//
// 못 먹는 재료는 두 종류다.
//   ALLERGY(알레르기) - AI 추천에서 반드시 뺀다
//   DISLIKE(싫어함)   - 가능하면 피하고 대체 재료를 알려준다
//
// user, onLogout, onUserChange 는 App.jsx 가 props 로 넘겨준다.
export default function MyPage({ user, onLogout, onUserChange }) {
  const navigate = useNavigate()

  const [profile, setProfile] = useState(null) // 서버가 준 내 정보 (요리 횟수 등 포함)
  const [bans, setBans] = useState([]) // 못 먹는 재료 목록
  const [loading, setLoading] = useState(true)

  // 재료 추가 입력칸
  const [banName, setBanName] = useState('')
  const [banType, setBanType] = useState('DISLIKE')
  const [error, setError] = useState('')

  // 내 정보 받아오기
  const loadProfile = async () => {
    try {
      const data = await getProfile()
      setProfile(data)
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
    }
  }

  // 못 먹는 재료 목록 받아오기
  const loadBans = async () => {
    try {
      const data = await getBans()
      setBans(data)
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
      setLoading(false)
    }
  }

  // 화면이 처음 열릴 때 한 번
  useEffect(() => {
    loadProfile()
    loadBans()
  }, [])

  // 정보 수정 폼에서 저장했을 때. 바뀐 닉네임을 상단바에도 반영한다.
  const handleProfileSaved = (nickname) => {
    loadProfile()
    onUserChange({ ...user, nickname: nickname })
  }

  // 재료 추가
  const handleAddBan = async (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    const name = banName.trim()

    if (name === '') {
      setError('재료 이름을 입력해주세요')
      return
    }

    // 이미 등록한 재료인지 목록에서 찾아본다
    const already = bans.find((ban) => ban.ingredient_name === name)
    if (already) {
      setError('이미 등록한 재료입니다')
      return
    }

    setError('')

    try {
      const created = await addBan({ ingredientName: name, banType: banType })
      setBans([...bans, created]) // 서버가 돌려준 재료를 목록 뒤에 붙인다
      setBanName('')
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
    }
  }

  // 재료 삭제
  const handleRemoveBan = async (ban) => {
    try {
      await removeBan(ban.id)
      setBans(bans.filter((item) => item.id !== ban.id)) // 지운 것만 빼고 남긴다
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
    }
  }

  // 탈퇴가 끝나면 로그아웃하고 홈으로
  const handleWithdrawn = () => {
    onLogout()
    navigate('/')
  }

  // 알레르기로 등록한 재료 수
  const allergyCount = bans.filter((ban) => ban.ban_type === 'ALLERGY').length

  return (
    <div className="mypage">
      {/* 1. 내 정보 */}
      <section className="card">
        <h2 className="section-title">내 정보</h2>
        <dl className="mypage__info">
          <dt>닉네임</dt>
          <dd>{user.nickname}</dd>
          <dt>이메일</dt>
          <dd>{user.email}</dd>
          <dt>가입일</dt>
          <dd>{formatDate(user.created_at)}</dd>
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

      {/* 2. 정보 수정. 내 정보를 받아온 다음에 보여준다 (입력칸을 그 값으로 채우려고) */}
      {profile && <ProfileForm profile={profile} onSaved={handleProfileSaved} />}

      {/* 3. 못 먹는 재료 */}
      <section className="card">
        <h2 className="section-title">못 먹는 재료</h2>
        <p className="muted mypage__desc">
          여기 등록한 재료는 AI 추천과 레시피에서 제외됩니다.
          {allergyCount > 0 && ` 알레르기로 등록한 ${allergyCount}개는 대체 재료 없이 완전히 제외합니다.`}
        </p>

        <form className="mypage__form" onSubmit={handleAddBan}>
          <input
            type="text"
            value={banName}
            onChange={(e) => setBanName(e.target.value)}
            placeholder="재료 이름 (예: 오이)"
            maxLength={100}
          />
          <select value={banType} onChange={(e) => setBanType(e.target.value)}>
            <option value="DISLIKE">싫어함</option>
            <option value="ALLERGY">알레르기</option>
          </select>
          <button type="submit" className="btn">
            추가
          </button>
        </form>
        <ErrorMessage message={error} />

        {loading && <p className="empty">불러오는 중…</p>}
        {!loading && bans.length === 0 && <p className="empty">등록한 재료가 없습니다.</p>}

        {!loading && bans.length > 0 && (
          <ul className="mypage__bans">
            {bans.map((ban) => (
              <li key={ban.id} className="mypage__ban">
                {ban.ban_type === 'ALLERGY' ? (
                  <span className="tag tag--allergy">알레르기</span>
                ) : (
                  <span className="tag tag--dislike">싫어함</span>
                )}
                <span className="mypage__ban-name">{ban.ingredient_name}</span>
                <button type="button" className="mypage__remove" onClick={() => handleRemoveBan(ban)}>
                  ✕
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 4. 비밀번호 변경 · 탈퇴 */}
      <AccountSection onWithdrawn={handleWithdrawn} />

      {/* 백엔드가 연결됐는지 확인하는 개발용 카드 */}
      <ApiStatus />
    </div>
  )
}
