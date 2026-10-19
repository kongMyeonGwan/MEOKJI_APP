import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ApiStatus from './components/ApiStatus'
import ErrorMessage from '../../components/common/ErrorMessage'
import ProfileForm from './components/ProfileForm'
import AccountSection from './components/AccountSection'
import { getProfile } from '../../api/users'
import { formatDate } from '../../utils/format'

// 마이페이지. 위에서부터 이렇게 쌓인다.
//   1. 내 정보 (이 파일)
//   2. 정보 수정 폼     → components/ProfileForm.jsx
//   3. 비밀번호 · 탈퇴  → components/AccountSection.jsx
//
// user, onLogout, onUserChange 는 App.jsx 가 props 로 넘겨준다.
export default function MyPage({ user, onLogout, onUserChange }) {
  const navigate = useNavigate()

  const [profile, setProfile] = useState(null) // 서버가 준 내 정보 (요리 횟수 등 포함)

  // 재료 추가 입력칸
  const [error, setError] = useState('')

  // 내 정보 받아오기
  const loadProfile = async () => {
    setError('')
    try {
      const data = await getProfile()
      setProfile(data)
    } catch (err) {
      // 서버가 보낸 오류 문장을 그대로 보여준다.
      if (err.response) {
        // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '닉네임을 입력해주세요' }]
        const detail = err.response.data.detail
        if (Array.isArray(detail)) {
          setError(detail[0]?.msg || '요청을 처리하지 못했습니다.')
        } else {
          setError(detail || '요청을 처리하지 못했습니다.')
        }
      } else if (err.request) {
        // 백엔드를 안 켰거나 30초가 지난 경우
        setError('서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.')
      } else {
        // 그 밖의 에러 (예: api/reviews.js 의 "게시판 기능은 아직 준비 중입니다.")
        setError(err.message || '내 정보를 불러오지 못했습니다.')
      }
    }
  }

  // 화면이 처음 열릴 때 한 번
  useEffect(() => {
    loadProfile()
  }, [])

  // 정보 수정 폼에서 저장했을 때. 바뀐 닉네임을 상단바에도 반영한다.
  const handleProfileSaved = (nickname) => {
    loadProfile()
    onUserChange({ ...user, nickname })
  }

  // 탈퇴가 끝나면 로그아웃하고 홈으로
  const handleWithdrawn = () => {
    onLogout()
    navigate('/')
  }

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

        <ErrorMessage message={error} />
      </section>

      {/* 2. 정보 수정. 내 정보를 받아온 다음에 보여준다 (입력칸을 그 값으로 채우려고) */}
      {profile && <ProfileForm profile={profile} onSaved={handleProfileSaved} />}
      
      {/* 3. 비밀번호 변경 · 탈퇴 */}
      <AccountSection onWithdrawn={handleWithdrawn} />

      {/* 백엔드가 연결됐는지 확인하는 개발용 카드 */}
      <ApiStatus />
    </div >
  )
}
