import { useEffect } from 'react'
import { createPortal } from 'react-dom'

// 화면 가운데 뜨는 창.
//   onClose  : 닫을 때 부를 함수 (바깥을 누르거나 ESC 를 누르면 불린다)
//   children : 창 안에 보여줄 내용
//
// 쓰는 쪽에서 {open && <Modal onClose={...}>...</Modal>} 처럼 조건부로 그린다.
//
// 안에 내용을 두지 않는 이유 — 쓰는 자리마다 내용이 다르다
// (삭제 확인 · 비밀번호 변경 · 사진 크게 보기). 틀만 맡고
// 안은 children 으로 받는다.
//
// 틀이 맡는 일 셋
//   1. 가운데 띄우기 (CSS)
//   2. 뒤쪽 화면 스크롤 막기
//   3. ESC 와 바깥 클릭으로 닫기
//
// 2 · 3 번이 이 컴포넌트가 필요한 이유다. 화면마다 적으면 한 곳에서
// 빠뜨려도 티가 안 난다 - 창은 떠 있는데 뒤쪽이 스크롤되거나,
// ESC 가 안 먹는다.
export default function Modal({ onClose, children }) {
  // 창이 떠 있는 동안: 뒤쪽 화면 스크롤 막기 + ESC 키로 닫기
  useEffect(() => {
    // 뒤쪽 화면 스크롤 막기.
    //
    // 안 막으면 창 위에서 휠을 굴릴 때 뒤쪽이 움직인다. 창을 닫고
    // 보면 엉뚱한 자리로 가 있다
    document.body.style.overflow = 'hidden'

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)

    // 창이 사라지면 원래대로
    //
    // 스크롤을 안 돌려놓으면 창을 닫은 뒤에도 화면이 안 움직인다.
    // 이벤트를 안 떼면 창을 드나들 때마다 쌓여서, ESC 한 번에
    // onClose 가 여러 번 불린다
    //
    // '' 로 되돌리는 이유 — 'auto' 로 넣으면 원래 CSS 에 적어둔 값을
    // 덮어쓴다. 빈 글자는 "이 속성을 안 적은 상태" 로 돌려놓는다
    return () => {
      document.body.style.overflow = ''
      document.removeEventListener('keydown', handleKeyDown)
    }
    // onClose 가 바뀌면 다시 건다. 쓰는 쪽에서 그 자리에 함수를
    // 적으면(onClose={() => ...}) 다시 그려질 때마다 새 함수가 되어
    // 이 useEffect 도 매번 다시 도는데, 하는 일이 가벼워 문제는 없다
  }, [onClose])

  // document.body 에 그린다 → 부모 카드의 overflow 에 잘리지 않는다
  //
  // createPortal 은 "이 자리에 적었지만 다른 데 그려라" 는 뜻이다.
  // 모달을 쓰는 자리가 overflow: hidden 인 카드 안이면, 보통대로
  // 그리면 창이 카드 밖으로 못 나가 잘린다.
  //
  // z-index 를 아무리 올려도 안 된다 - 잘리는 것은 쌓임 순서가
  // 아니라 부모가 넘치는 부분을 감추기 때문이다
  return createPortal(
    // 배경 전체에 onClick 을 건다. 바깥을 눌러 닫는 자리다
    <div className="modal" onClick={onClose}>
      {/* 창 안을 눌렀을 때는 바깥(배경)까지 클릭이 전달되지 않게 막는다 */}
      {/* stopPropagation 이 없으면 창 안을 눌러도 닫힌다.
          클릭이 안쪽에서 바깥으로 거슬러 올라가며 부모의 onClick 까지
          부르기 때문이다(이벤트 버블링).

          role · aria-modal 은 화면 읽어주는 프로그램에게
          "이것은 떠 있는 창이다" 를 알려주는 표시다 */}
      <div className="modal__box card" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>,
    document.body
  )
}
