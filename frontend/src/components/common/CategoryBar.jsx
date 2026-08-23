import { useRef } from 'react'
import FoodIllustration from './FoodIllustration'
import { MOCK_CATEGORIES } from '../../mocks'
import '../../styles/components/common/CategoryBar.css'

/**
 * 카테고리 가로 스크롤 칩.
 *
 * 칩이 화면 너비를 넘으면 가로로 스크롤된다. 스크롤바가 안 보이는 환경
 * (마우스만 쓰는 데스크톱)을 위해 오른쪽 끝에 넘김 버튼을 둔다.
 *
 * 선택은 부모가 들고 있는다. 이 컴포넌트는 그리기만 한다.
 */
export default function CategoryBar({ value, onChange }) {
  const listRef = useRef(null)

  const scrollNext = () => {
    const el = listRef.current
    if (!el) return
    // 한 번에 화면 너비의 3/4씩. 전부 넘기면 어디까지 봤는지 놓친다
    el.scrollBy({ left: el.clientWidth * 0.75, behavior: 'smooth' })
  }

  return (
    <div className="cats">
      <ul className="cats__list" ref={listRef}>
        {MOCK_CATEGORIES.map((cat) => (
          <li key={cat.key}>
            <button
              type="button"
              className={`cats__chip${value === cat.key ? ' is-active' : ''}`}
              onClick={() => onChange(cat.key)}
              aria-pressed={value === cat.key}
            >
              <span className="cats__thumb">
                {cat.art ? (
                  <FoodIllustration art={cat.art} className="cats__art" />
                ) : (
                  // '전체'는 그릴 음식이 없어 격자 아이콘을 쓴다
                  <svg viewBox="0 0 24 24" className="cats__all" aria-hidden="true">
                    <rect x="4" y="4" width="6.5" height="6.5" rx="1.6" />
                    <rect x="13.5" y="4" width="6.5" height="6.5" rx="1.6" />
                    <rect x="4" y="13.5" width="6.5" height="6.5" rx="1.6" />
                    <rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1.6" />
                  </svg>
                )}
              </span>
              <span className="cats__label">{cat.label}</span>
            </button>
          </li>
        ))}
      </ul>

      <button
        type="button"
        className="cats__next"
        onClick={scrollNext}
        aria-label="카테고리 더 보기"
      >
        ›
      </button>
    </div>
  )
}
