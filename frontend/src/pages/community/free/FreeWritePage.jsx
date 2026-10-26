import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ErrorMessage from '../../../components/common/ErrorMessage'
import useForm from '../../../hooks/useForm'
import { createFreePost, uploadFreePostFile } from '../../../api/freePosts'

const CONTENT_MAX = 2000 // 본문 최대 글자 수

// 자유게시판 글쓰기 (/free/write). 로그인해야 들어올 수 있다.
//
// user 를 받지 않는다. App.jsx 의 라우터가 loginOnly 로 이 화면을 감싸서,
// 로그인 안 한 사람은 여기까지 오지 못하고 로그인 화면으로 보내진다.
// 글쓴이가 누구인지도 받을 필요가 없다 — 서버가 토큰에서 꺼내 쓴다.
//
// isMine 을 쓸 일도 없다. 아직 글이 없으니 주인을 따질 것이 없고,
// 등록하고 나면 상세 화면(FreeDetailPage)으로 넘어가 거기서 따진다.
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

  // 사진 입력칸. 지우기 버튼이 이 칸의 값도 같이 비워야 한다
  const fileInputRef = useRef(null)

  const navigate = useNavigate()

  // 미리보기 주소가 바뀌거나 화면을 떠나면, 쓰던 주소를 지워 메모리를 돌려준다.
  // return 으로 넘긴 함수가 그 '치우는 일' 이다 (리액트가 알아서 불러준다)
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

  // 고른 사진 지우기 (미리보기 오른쪽 위의 ✕)
  const handleFileRemove = () => {
    setFile(null)
    setPhotoUrl('') // 주소가 바뀌면 위 useEffect 가 옛 주소를 지워준다

    // 입력칸도 비운다. 안 비우면 같은 사진을 다시 골랐을 때 값이 그대로라
    // onChange 가 울리지 않아 미리보기가 돌아오지 않는다
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
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

      // 2. 사진이 있으면 따로 올린다 (글이 있어야 어디에 붙일지 정해진다).
      //    글과 사진을 한 번에 못 보내는 이유 — 사진은 FormData, 글은 JSON 이라
      //    보내는 모양이 다르고, 서버의 첨부 주소가 /{글번호}/file 이라 글 번호가
      //    먼저 나와야 한다
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
          <p
            className={values.content.length > CONTENT_MAX - 100 ? 'free-write__count-warn' : 'muted free-write__help'}
          >
            {values.content.length} / {CONTENT_MAX}
          </p>
        </label>

        <label className="free-write__field">
          <span>사진 (선택)</span>
          <input
            type="file"
            ref={fileInputRef}
            accept=".jpg,.jpeg,.png,.gif,.webp"
            onChange={handleFileChange}
          />
          <p className="muted free-write__help">
            {file ? `${file.name} — 등록하면 함께 올라갑니다` : '한 장까지 올릴 수 있어요. 5MB 이하 이미지.'}
          </p>
        </label>

        {/* 고른 사진 미리보기. label 밖에 둔다 — 안에 두면 ✕ 를 눌러도
            label 이 파일 선택창까지 같이 열어버린다 */}
        {photoUrl && (
          <div className="free-write__preview-box">
            <img className="free-write__preview" src={photoUrl} alt="올릴 사진 미리보기" />
            <button
              type="button"
              className="free-write__preview-remove"
              onClick={handleFileRemove}
              title="사진 지우기"
              aria-label="사진 지우기"
            >
              ✕
            </button>
          </div>
        )}

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
