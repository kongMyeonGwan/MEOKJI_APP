import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorMessage from '../../components/common/ErrorMessage'
import useForm from '../../hooks/useForm'

const CONTENT_MAX = 2000 // 본문 최대 글자 수

// 자유게시판 글쓰기 (/free/write). 로그인해야 들어올 수 있다.
//
// 레시피 후기 글쓰기(/community/write)와 다른 점
//   후기는 "공유할 내 기록" 드롭다운에서 식사 기록을 하나 골라야 했지만,
//   여기는 고를 것이 없다. 제목 · 내용 · 사진(선택) 세 칸뿐이다.
//
// TODO(자유게시판 담당): 지금은 등록을 누르면 "준비 중" 안내만 뜨는 퍼블리싱 상태다.
//   1. api/freePosts.js 에 createFreePost · uploadFreePostFile 을 만든다
//   2. handleSubmit 의 alert 를 아래 순서로 바꾼다
//        const created = await createFreePost({ title, content })
//        if (file) { await uploadFreePostFile(created.id, file) }
//        navigate(`/free/${created.id}`)
//      (useNavigate 를 import 해서 쓰고, catch 는 CODE_STYLE.md 2번을 그대로 복사한다)
export default function FreeWritePage() {
  const { values, handleChange } = useForm({ title: '', content: '' })
  const [file, setFile] = useState(null) // 고른 사진 파일
  const [photoUrl, setPhotoUrl] = useState('') // 고른 사진 미리보기 주소
  const [error, setError] = useState('')

  // 미리보기 주소가 바뀌거나 화면을 떠나면, 쓰던 주소를 지워 메모리를 돌려준다
  useEffect(() => {
    return () => {
      if (photoUrl) {
        URL.revokeObjectURL(photoUrl)
      }
    }
  }, [photoUrl])

  // 사진을 골랐을 때. 5MB 가 넘으면 안 받는다.
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
  const handleSubmit = (e) => {
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
    alert('자유게시판 기능은 아직 준비 중입니다.')
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
          <button type="submit" className="btn">
            등록
          </button>
        </div>
      </form>
    </div>
  )
}
