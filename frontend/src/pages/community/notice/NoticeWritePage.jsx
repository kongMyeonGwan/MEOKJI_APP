import { Link } from 'react-router-dom'
import NoticeForm from './components/NoticeForm'

// 공지 작성 (/notice/write). 관리자만 쓸 수 있다.
// 폼 자체는 상세 화면의 수정 폼과 같은 조각(components/NoticeForm)을 쓴다.
//
// 퍼블리싱만 된 화면이다. 등록은 아직 아무 일도 하지 않는다.
//
// TODO(공지 담당)
//   - 관리자가 아니면 들어오지 못하게 막는다
//     (App.jsx 에서 loginOnly() 처럼 adminOnly() 를 만들어 감싸는 자리다)
//   - 등록은 NoticeForm 안에서 createNotice(form) 을 부르고, 끝나면 쓴 공지로 보낸다
export default function NoticeWritePage() {
  return (
    <div className="card write">
      <div className="write__head">
        <h2 className="section-title" style={{ margin: 0 }}>
          공지 작성
        </h2>
        <Link to="/notice" className="muted write__cancel">
          취소
        </Link>
      </div>

      {/* notice 를 안 넘기면 '새로 쓰기' 모양이 된다 */}
      <NoticeForm cancelTo="/notice" />
    </div>
  )
}
