import '../../styles/components/common/Pagination.css'

// 페이지 번호 버튼.
//
//      ‹  1  2  [3]  4  5  ›
//
// 서버가 준 total_pages 를 그대로 쓴다. 목록은 20건만 받아오기 때문에
// 프론트에서 전체 건수를 셀 방법이 없다.
//
// 글이 2000건이면 페이지가 100개라서 번호를 다 그리면 화면을 덮는다.
// 그래서 지금 페이지 둘레만 그리고 나머지는 '...' 로 접는다.
// 첫 페이지랑 끝 페이지는 항상 보여준다.

// 지금 페이지 양옆으로 몇 개씩 보여줄지. 2 면 1 2 [3] 4 5 가 된다.
const WINDOW = 2

// 화면에 그릴 항목을 만든다. 숫자는 페이지 번호, '...' 는 접힌 자리다.
//   makeItems(1, 1)    -> [1]
//   makeItems(3, 5)    -> [1, 2, 3, 4, 5]
//   makeItems(50, 100) -> [1, '...', 48, 49, 50, 51, 52, '...', 100]
export function makeItems(page, totalPages) {
  const pages = []

  for (let n = 1; n <= totalPages; n = n + 1) {
    // 첫 페이지, 끝 페이지, 지금 페이지 둘레만 남긴다
    if (n === 1 || n === totalPages || Math.abs(n - page) <= WINDOW) {
      pages.push(n)
    }
  }

  // 번호가 건너뛴 자리에 '...' 를 넣는다.
  // 딱 한 칸만 비었으면(5 다음이 7) '...' 대신 그 번호를 그냥 쓴다.
  // '...' 도 한 칸을 차지해서 접어봐야 좁아지지도 않기 때문이다.
  const items = []

  for (let i = 0; i < pages.length; i++) {
    const n = pages[i]
    const previous = pages[i - 1]

    if (previous !== undefined && n - previous > 1) {
      if (n - previous === 2) {
        items.push(n - 1)
      } else {
        items.push('...')
      }
    }

    items.push(n)
  }

  return items
}

export default function Pagination({ page, totalPages, onChange }) {
  // 한 페이지뿐이면 버튼을 안 그린다. "1 / 1" 은 누를 데가 없다.
  if (totalPages <= 1) {
    return null
  }

  // 범위를 벗어나면 무시한다. 1페이지에서 ‹ 를 눌러도 0페이지로 안 간다.
  function go(next) {
    if (next >= 1 && next <= totalPages && next !== page) {
      onChange(next)
    }
  }

  const items = makeItems(page, totalPages)

  return (
    <nav className="pager" aria-label="페이지">
      <button
        type="button"
        className="pager__btn"
        onClick={() => go(page - 1)}
        disabled={page === 1}
        aria-label="이전 페이지"
      >
        ‹
      </button>

      {items.map(function (item, i) {
        if (item === '...') {
          // key 에 순서 번호를 쓴다. '...' 는 여러 개라서 값으로는 구분이 안 된다.
          return (
            <span key={'gap-' + i} className="pager__gap" aria-hidden="true">
              …
            </span>
          )
        }

        let className = 'pager__btn'
        if (item === page) {
          className = 'pager__btn is-active'
        }

        let current = undefined
        if (item === page) {
          current = 'page'
        }

        return (
          <button
            key={item}
            type="button"
            className={className}
            onClick={() => go(item)}
          >
            {item}
          </button>
        )
      })}

      <button
        type="button"
        className="pager__btn"
        onClick={() => go(page + 1)}
        disabled={page === totalPages}
        aria-label="다음 페이지"
      >
        ›
      </button>
    </nav>
  )
}
