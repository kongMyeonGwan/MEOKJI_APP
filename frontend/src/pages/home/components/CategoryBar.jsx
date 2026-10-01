import { useRef } from 'react'
import FoodIllustration from '../../../components/common/FoodIllustration'
import './CategoryBar.css'

// 카테고리 칩을 가로로 늘어놓는다. 넘치면 옆으로 스크롤된다.
//   categories : [{ key: 'KR', label: '한식', art: 'stew' }, ...]
//   value      : 지금 고른 카테고리 key
//   onChange   : 칩을 눌렀을 때 부를 함수
export default function CategoryBar({ categories, value, onChange }) {
  // <ul> 태그를 직접 잡아두는 ref. 오른쪽 › 버튼으로 스크롤할 때 쓴다.
  const listRef = useRef(null)

  const scrollNext = () => {
    listRef.current.scrollBy({ left: 300, behavior: 'smooth' })
  }

  return (
    <div className="cats">
      <ul className="cats__list" ref={listRef}>
        {categories.map((cat) => (
          <li key={cat.key}>
            <button
              type="button"
              className={value === cat.key ? 'cats__chip is-active' : 'cats__chip'}
              onClick={() => onChange(cat.key)}
            >
              <span className="cats__thumb">
                {cat.art ? (
                  <FoodIllustration art={cat.art} className="cats__art" />
                ) : (
                  // '전체' 는 그림 대신 네모 4개 아이콘
                  <svg viewBox="0 0 24 24" className="cats__all">
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

      <button type="button" className="cats__next" onClick={scrollNext}>
        ›
      </button>
    </div>
  )
}
