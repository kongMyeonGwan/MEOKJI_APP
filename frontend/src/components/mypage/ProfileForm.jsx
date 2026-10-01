import { useState } from 'react'
import AddressPicker from '../common/AddressPicker'
import ErrorMessage from '../common/ErrorMessage'
import useForm from '../../hook/useForm'
import { updateProfile } from '../../api/users'

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
  const { values, handleChange } = useForm({
    nickname: profile.nickname,
    gender: profile.gender || '',
  })

  // 사는 곳. AddressPicker 가 두 값을 같이 바꿔서 따로 둔다
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

    if (values.nickname.trim() === '') {
      setError('닉네임을 입력해주세요')
      return
    }

    setSaving(true)
    setError('')
    setDone(false)

    try {
      await updateProfile({
        nickname: values.nickname.trim(),
        // 선택 안 한 항목(빈 글자)은 null 로 보내야 서버에서도 비워진다
        gender: values.gender || null,
        addressSido: sido || null,
        addressSigungu: sigungu || null,
      })
      setDone(true)
      onSaved(values.nickname.trim())
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
          <input type="text" name="nickname" value={values.nickname} onChange={handleChange} maxLength={50} />
        </label>

        <label className="mypage__field">
          <span>성별</span>
          <select name="gender" value={values.gender} onChange={handleChange}>
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
