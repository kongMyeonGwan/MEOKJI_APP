import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import '../../styles/components/common/Modal.css'

// 화면 가운데 뜨는 창.
//   onClose  : 닫을 때 부를 함수 (바깥을 누르거나 ESC 를 누르면 불린다)
//   children : 창 안에 보여줄 내용
//
// 쓰는 쪽에서 {open && <Modal onClose={...}>...</Modal>} 처럼 조건부로 그린다.
export default function Modal({ onClose, children }) {
  // 창이 떠 있는 동안: 뒤쪽 화면 스크롤 막기 + ESC 키로 닫기
  useEffect(() => {
    document.body.style.overflow = 'hidden'

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)

    // 창이 사라지면 원래대로
    return () => {
      document.body.style.overflow = ''
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [onClose])

  // document.body 에 그린다 → 부모 카드의 overflow 에 잘리지 않는다
  return createPortal(
    <div className="modal" onClick={onClose}>
      {/* 창 안을 눌렀을 때는 바깥(배경)까지 클릭이 전달되지 않게 막는다 */}
      <div className="modal__box card" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>,
    document.body
  )
}
