import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../../../components/common/ErrorMessage'
import useForm from '../../../hooks/useForm'
import { createFreePost, uploadFreePostFile } from '../../../api/freePosts'

const CONTENT_MAX = 2000 // 본문 최대 글자 수

// 자유게시판 글쓰기 (/free/write). 로그인해야 들어올 수 있다.
//
// 레시피 후기 글쓰기(/community/write)와 다른 점
//   후기는 "공유할 내 기록" 드롭다운에서 식사 기록을 하나 골라야 했지만,
//   여기는 고를 것이 없다. 제목 · 내용 · 사진(선택) 세 칸뿐이다.
export default function FreeWritePage() {
  const { values, handleChange } = useForm({ title: '', content: '' })
  const [file, setFile] = useState(null) // 고른 사진 파일
  const [photoUrl, setPhotoUrl] = useState('') // 고른 사진 미리보기 주소
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const navigate = useNavigate()

  // 미리보기 주소가 바뀌거나 화면을 떠나면, 쓰던 주소를 지워 메모리를 돌려준다
  useEffect(() => {
    return () => {
      if (photoUrl) {
        URL.revokeObjectURL(photoUrl)
      }
    }
  }, [photoUrl])

  // 사진을 골랐을 때. 5MB 가 넘으면 안 받는다 (서버도 같은 값으로 막는다)
  const handleFileChange = (e) => {
    const picked = e.target.files[0]

    if (picked && picked.size > 5 * 1024 * 1024) {
      alert('5MB 이하 사진만 올릴 수 있어요')
      e.target.value = '' // 입력칸 비우기
      setFile(null)
      setPhotoUrl('')
      return
    }

    setFile(picked || null)
    // 파일이 있으면 브라우저 안에서만 쓰는 임시 주소를 만들어 <img> 로 보여준다
    setPhotoUrl(picked ? URL.createObjectURL(picked) : '')
  }

  // 등록 버튼
  const handleSubmit = async (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    if (!values.title.trim()) {
      setError('제목을 입력해주세요')
      return
    }
    if (!values.content.trim()) {
      setError('내용을 입력해주세요')
      return
    }

    setError('')
    setSaving(true)
    try {
      // 1. 글 등록
      const created = await createFreePost({
        title: values.title.trim(),
        content: values.content,
      })

      // 2. 사진이 있으면 따로 올린다 (글이 있어야 어디에 붙일지 정해진다)
      if (file) {
        await uploadFreePostFile(created.id, file)
      }

      // 방금 쓴 글로 이동
      navigate(`/free/${created.id}`)
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
        setError(err.message)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="card free-write">
      <div className="free-write__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          자유게시판 글쓰기
        </h2>
        <Link to="/free" className="muted free-write__cancel">
          취소
        </Link>
      </div>

      <form onSubmit={handleSubmit}>
        <label className="free-write__field">
          <span>제목</span>
          <input
            type="text"
            name="title"
            value={values.title}
            onChange={handleChange}
            placeholder="어떤 이야기인가요?"
            maxLength={200}
          />
        </label>

        <label className="free-write__field">
          <span>내용</span>
          <textarea
            name="content"
            value={values.content}
            onChange={handleChange}
            placeholder="무슨 이야기든 괜찮아요. 요리 이야기가 아니어도 됩니다."
            maxLength={CONTENT_MAX}
            rows={12}
          />
          {/* 거의 다 차면 빨간 글씨로 알려준다 */}
          <p className={values.content.length > CONTENT_MAX - 100 ? 'free-write__count-warn' : 'muted free-write__help'}>
            {values.content.length} / {CONTENT_MAX}
          </p>
        </label>

        <label className="free-write__field">
          <span>사진 (선택)</span>
          <input type="file" accept=".jpg,.jpeg,.png,.gif,.webp" onChange={handleFileChange} />
          <p className="muted free-write__help">
            {file ? `${file.name} — 등록하면 함께 올라갑니다` : '한 장까지 올릴 수 있어요. 5MB 이하 이미지.'}
          </p>
          {photoUrl && <img className="free-write__preview" src={photoUrl} alt="올릴 사진 미리보기" />}
        </label>

        <ErrorMessage message={error} />

        <div className="free-write__actions">
          <Link to="/free" className="btn btn--ghost">
            목록
          </Link>
          <button type="submit" className="btn" disabled={saving}>
            {saving ? '등록 중…' : '등록'}
          </button>
        </div>
      </form>
    </div>
  )
}
