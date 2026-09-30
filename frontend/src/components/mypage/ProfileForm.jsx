import { useState } from 'react'
import AddressPicker from '../common/AddressPicker'
import ErrorMessage from '../common/ErrorMessage'
import { updateProfile } from '../../api/users'
import { getErrorMessage } from '../../utils/apiError'

// 내 정보 수정 폼 (닉네임, 성별, 사는 곳). 마이페이지에서 쓴다.
//   profile : 서버에서 받은 내 정보. 입력칸을 이 값으로 채워서 시작한다.
//   onSaved : 저장에 성공하면 부를 함수. 바뀐 닉네임을 넘겨준다.

// 성별 선택지. 서버(DB)에 저장되는 값과 같아야 한다.
const GENDERS = [
  { value: '', label: '선택 안 함' },
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'OTHER', label: '기타' },
]

export default function ProfileForm({ profile, onSaved }) {
  // 서버 값이 null 이면 빈 글자('')로 바꿔서 넣는다 (입력칸에는 null 을 못 넣는다)
  const [nickname, setNickname] = useState(profile.nickname)
  const [gender, setGender] = useState(profile.gender || '')
  const [sido, setSido] = useState(profile.address_sido || '')
  const [sigungu, setSigungu] = useState(profile.address_sigungu || '')

  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false) // 저장 완료 문구를 보여줄지

  // 주소를 골랐을 때 (AddressPicker 가 시/도, 시/군/구를 같이 알려준다)
  const changeAddress = (nextSido, nextSigungu) => {
    setSido(nextSido)
    setSigungu(nextSigungu)
  }

  const handleSubmit = async (e) => {
    e.preventDefault() // 폼을 제출하면 페이지가 새로고침되는데, 그걸 막는다

    if (nickname.trim() === '') {
      setError('닉네임을 입력해주세요')
      return
    }

    setSaving(true)
    setError('')
    setDone(false)

    try {
      await updateProfile({
        nickname: nickname.trim(),
        // 선택 안 한 항목(빈 글자)은 null 로 보내야 서버에서도 비워진다
        gender: gender || null,
        addressSido: sido || null,
        addressSigungu: sigungu || null,
      })
      setDone(true)
      onSaved(nickname.trim())
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="card">
      <h2 className="section-title">정보 수정</h2>
      <p className="muted mypage__desc">
        이메일은 로그인 아이디라 바꿀 수 없습니다. 사는 곳을 골라두면 그 지역 날씨로 메뉴를 추천해드려요.
      </p>

      <form className="mypage__profile" onSubmit={handleSubmit}>
        <label className="mypage__field">
          <span>닉네임</span>
          <input type="text" value={nickname} onChange={(e) => setNickname(e.target.value)} maxLength={50} />
        </label>

        <label className="mypage__field">
          <span>성별</span>
          <select value={gender} onChange={(e) => setGender(e.target.value)}>
            {GENDERS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>

        <div className="mypage__field">
          <span>사는 곳</span>
          <AddressPicker sido={sido} sigungu={sigungu} onChange={changeAddress} disabled={saving} />
        </div>

        <div className="mypage__field-actions">
          <button type="submit" className="btn" disabled={saving}>
            {saving ? '저장 중…' : '저장'}
          </button>
          {done && <span className="muted">저장했습니다.</span>}
        </div>
      </form>

      <ErrorMessage message={error} />
    </section>
  )
}
