import { useRef } from 'react'
import FoodIllustration from './FoodIllustration'
import { useCatalog } from '../../contexts/CatalogContext'
import '../../styles/components/common/CategoryBar.css'

// 카테고리 칩을 가로로 쭉 늘어놓는다.
// 화면 너비를 넘으면 옆으로 스크롤되는데, 스크롤바가 안 보이는 환경도 있어서
// 오른쪽 끝에 넘기는 버튼을 하나 뒀다.
//
// 지금 뭐가 선택됐는지는 부모가 들고 있고 여기는 그리기만 한다.
export default function CategoryBar({ value, onChange }) {
  const listRef = useRef(null)
  const { categories } = useCatalog()

  // 오른쪽 › 를 누르면 칩 목록을 옆으로 밀어준다
  function scrollNext() {
    // useRef 로 잡아둔 <ul> 이다. 아직 화면에 없으면 null 이다.
    const list = listRef.current
    if (!list) {
      return
    }

    // 한 번에 화면 너비의 3/4 씩 민다.
    // 한 화면을 통째로 넘기면 어디까지 봤는지 놓친다.
    list.scrollBy({ left: list.clientWidth * 0.75, behavior: 'smooth' })
  }

  return (
    <div className="cats">
      <ul className="cats__list" ref={listRef}>
        {categories.map(function (cat) {
          let chipClass = 'cats__chip'
          if (value === cat.key) {
            chipClass = 'cats__chip is-active'
          }

          return (
            <li key={cat.key}>
              <button
                type="button"
                className={chipClass}
                onClick={() => onChange(cat.key)}
              >
                <span className="cats__thumb">
                  {cat.art ? (
                    <FoodIllustration art={cat.art} className="cats__art" />
                  ) : (
                    // '전체' 는 그릴 음식이 없어서 네모 네 개 아이콘을 쓴다
                    <svg
                      viewBox="0 0 24 24"
                      className="cats__all"
                      aria-hidden="true"
                    >
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
          )
        })}
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
