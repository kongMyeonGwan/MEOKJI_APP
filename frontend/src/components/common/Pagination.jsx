import '../../styles/components/common/Pagination.css'

/**
 * 페이지 번호 버튼.
 *
 *      ‹  1  2  [3]  4  5  ›
 *
 * 서버가 내려준 total_pages 를 그대로 쓴다. 전체 건수를 프론트가 다시
 * 계산하지 않는다 — 목록은 20건만 받아오므로 애초에 셀 방법도 없다.
 *
 * 번호를 다 그리지 않는 이유
 * --------------------------
 * 글이 2000건이면 페이지가 100개다. 100개를 다 그리면 버튼이 화면을 덮는다.
 * 그래서 현재 페이지 둘레로 WINDOW 개만 그리고 나머지는 '…' 로 접는다.
 * 첫 페이지와 끝 페이지는 항상 보여준다 — 맨 앞/맨 뒤로 한 번에 가는 길이
 * 없으면 100페이지에서 1페이지로 돌아가는 데 클릭을 여러 번 해야 한다.
 */

// 현재 페이지 양옆으로 몇 개씩 보여줄지. 2면 [3] 기준 1 2 [3] 4 5 가 된다.
const WINDOW = 2

/**
 * 화면에 그릴 항목을 만든다. 숫자는 페이지 번호, '...' 는 접힌 구간이다.
 *
 *   makeItems(1, 1)    -> [1]
 *   makeItems(3, 5)    -> [1, 2, 3, 4, 5]
 *   makeItems(50, 100) -> [1, '...', 48, 49, 50, 51, 52, '...', 100]
 */
export function makeItems(page, totalPages) {
  const pages = []

  for (let n = 1; n <= totalPages; n += 1) {
    // 첫 페이지 · 끝 페이지 · 현재 페이지 둘레만 남긴다
    if (n === 1 || n === totalPages || Math.abs(n - page) <= WINDOW) {
      pages.push(n)
    }
  }

  // 번호가 건너뛴 자리에 '...' 를 끼워 넣는다.
  // 딱 한 칸만 비었으면(예: 5 다음이 7) '...' 대신 그 번호를 그대로 쓴다.
  // '...' 도 한 칸을 차지해서, 접어봐야 좁아지지 않는데 클릭만 못 하게 된다.
  const items = []

  pages.forEach((n, i) => {
    const previous = pages[i - 1]

    if (previous !== undefined && n - previous > 1) {
      if (n - previous === 2) {
        items.push(n - 1)
      } else {
        items.push('...')
      }
    }

    items.push(n)
  })

  return items
}

export default function Pagination({ page, totalPages, onChange }) {
  // 한 페이지뿐이거나 아예 없으면 버튼을 그리지 않는다.
  // "1 / 1" 은 누를 데가 없어서 자리만 차지한다.
  if (totalPages <= 1) return null

  /** 범위를 벗어나면 무시한다. 1페이지에서 ‹ 를 눌러도 0페이지로 안 간다. */
  function go(next) {
    if (next >= 1 && next <= totalPages && next !== page) {
      onChange(next)
    }
  }

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

      {makeItems(page, totalPages).map((item, i) =>
        item === '...' ? (
          // key 에 인덱스를 쓴다. '...' 는 여러 개일 수 있어서 값 자체로는
          // 구분이 안 되고, 목록 순서가 바뀌는 것도 아니라 문제가 없다.
          <span key={`gap-${i}`} className="pager__gap" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            className={`pager__btn${item === page ? ' is-active' : ''}`}
            onClick={() => go(item)}
            // 화면낭독기가 "현재 페이지" 라고 읽어준다
            aria-current={item === page ? 'page' : undefined}
          >
            {item}
          </button>
        )
      )}

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
