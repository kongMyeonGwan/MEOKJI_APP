import '../../styles/components/common/Pagination.css'

// 페이지 번호 버튼.   ‹  1  2  [3]  4  5  ›
// 페이지가 많으면 화면을 덮으니까 지금 페이지 앞뒤 2개씩만 보여준다.
export default function Pagination({ page, totalPages, onChange }) {
  // 한 페이지뿐이면 버튼이 필요 없다
  if (totalPages <= 1) {
    return null
  }

  // 보여줄 페이지 번호 만들기. 예) 지금 5페이지면 [3, 4, 5, 6, 7]
  const first = Math.max(1, page - 2)
  const last = Math.min(totalPages, page + 2)
  const numbers = []
  for (let n = first; n <= last; n++) {
    numbers.push(n)
  }

  return (
    <nav className="pager">
      <button
        type="button"
        className="pager__btn"
        onClick={() => onChange(page - 1)}
        disabled={page === 1}
      >
        ‹
      </button>

      {numbers.map((n) => (
        <button
          key={n}
          type="button"
          className={n === page ? 'pager__btn is-active' : 'pager__btn'}
          onClick={() => onChange(n)}
        >
          {n}
        </button>
      ))}

      <button
        type="button"
        className="pager__btn"
        onClick={() => onChange(page + 1)}
        disabled={page === totalPages}
      >
        ›
      </button>
    </nav>
  )
}
