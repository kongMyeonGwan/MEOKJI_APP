// 순위 왕관. 1 · 2 · 3등에 금 · 은 · 동으로 칠한다.
//
//   <Crown rank={1} className="gpage__crown" />
//   → class="gpage__crown gpage__crown--1"
//
// 색은 CSS 가 className--{rank} 로 칠한다. 메뉴 게임 화면의 플레이 통계와
// 월드컵 명예의 전당이 같이 쓴다.
export default function Crown({ rank, className }) {
  return (
    <svg className={`${className} ${className}--${rank}`} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 8.5l4.6 3.6L12 4.5l4.4 7.6L21 8.5 19.4 18H4.6z" />
      <rect x="4.6" y="19.2" width="14.8" height="2.3" rx="1" />
    </svg>
  )
}
