// 페이지 번호 버튼.   ‹  1  2  [3]  4  5  ›
// 페이지가 많으면 화면을 덮으니까 지금 페이지 앞뒤 2개씩만 보여준다.
//
// 페이지 번호를 들고 있지 않다. 지금 페이지(page)를 받고, 바뀌면
// onChange 로 알려주기만 한다 - 목록을 다시 받아오는 일은 쓰는
// 화면이 한다. 이 컴포넌트가 상태를 들고 있으면, 화면이 검색어를
// 바꿔 1페이지로 돌릴 때 이쪽 숫자가 안 따라온다.
//
// 쓰는 법
//   <Pagination page={page} totalPages={data.total_pages} onChange={setPage} />
//
// totalPages 는 서버가 준 값을 그대로 넘긴다. 화면이 전체 건수를
// 나눠 계산하면 서버와 어긋날 수 있다.
export default function Pagination({ page, totalPages, onChange }) {
  // 한 페이지뿐이면 버튼이 필요 없다.
  // 0 일 때도 걸린다(글이 아예 없는 경우)
  if (totalPages <= 1) {
    return null
  }

  // 보여줄 페이지 번호 만들기. 예) 지금 5페이지면 [3, 4, 5, 6, 7]
  //
  // Math.max(1, ...) 가 없으면 1페이지에서 [-1, 0, 1, 2, 3] 이 된다.
  // Math.min 이 없으면 마지막 페이지에서 없는 번호가 나온다.
  //
  // 앞뒤가 잘리면 버튼이 5개보다 적어진다(1페이지에서는 [1,2,3]).
  // 늘 5개로 맞추려면 한쪽이 잘린 만큼 다른 쪽을 늘려야 하는데,
  // 그러면 코드가 길어진다 - 지금은 잘리는 대로 둔다
  const first = Math.max(1, page - 2)
  const last = Math.min(totalPages, page + 2)
  const numbers = []
  for (let n = first; n <= last; n++) {
    numbers.push(n)
  }

  return (
    <nav className="pager">
      {/* 이전 페이지.
          1페이지에서는 disabled 로 막는다. 안 막으면 0페이지를
          요청해서 서버가 422 를 낸다 */}
      <button
        type="button"
        className="pager__btn"
        onClick={() => onChange(page - 1)}
        disabled={page === 1}
      >
        ‹
      </button>

      {/* key 는 리액트가 "어느 버튼이 어느 것인지" 를 가리는 값이다.
          번호가 그대로 열쇠라 index 를 쓸 이유가 없다 */}
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
