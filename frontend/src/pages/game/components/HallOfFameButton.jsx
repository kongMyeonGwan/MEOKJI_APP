import { useEffect, useState } from 'react'
import WorldcupRanking from './WorldcupRanking'

// '명예의 전당' 바로가기 버튼. 누르면 룰렛 당첨 창과 같은 모양의 창에 순위를 띄운다.
//
//   <HallOfFameButton highlight="김치찌개" refreshKey={n} />
//
//   highlight  : 강조할 음식 (우승 화면에서 방금 우승한 메뉴)
//   refreshKey : 바뀌면 다음에 열 때 순위를 새로 받아온다 (이번 우승이 저장된 뒤)
//
// 창은 ×, 바깥(어두운 곳), Esc 로 닫는다.
export default function HallOfFameButton({ highlight, refreshKey = 0 }) {
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!open) {
      return
    }
    const onKey = (e) => {
      if (e.key === 'Escape') {
        setOpen(false)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <>
      <button type="button" className="hof-btn" onClick={() => setOpen(true)}>
        <span className="hof-btn__trophy" aria-hidden="true">🏆</span>
        <span className="hof-btn__text">
          <strong>명예의 전당</strong>
          <small>우승 메뉴 TOP 5</small>
        </span>
      </button>

      {open && (
        <div className="roulette-win" onClick={() => setOpen(false)}>
          <div
            className="card roulette-win__box hof-modal"
            role="dialog"
            aria-modal="true"
            aria-label="명예의 전당"
            onClick={(e) => e.stopPropagation()}
          >
            <button type="button" className="roulette-win__close" onClick={() => setOpen(false)} aria-label="닫기">
              ×
            </button>

            <p className="roulette-win__kicker">음식 이상형 월드컵</p>
            <h3 className="roulette-win__title">🏆 명예의 전당</h3>
            <p className="muted hof-modal__desc">우승을 가장 많이 한 메뉴 TOP 5</p>

            <WorldcupRanking key={refreshKey} highlight={highlight} bare />
          </div>
        </div>
      )}
    </>
  )
}
