/**
 * 아이콘 세트 (인라인 SVG).
 *
 * 이모지를 쓰면 OS·브라우저마다 모양이 달라지고, 비활성 상태를 만들려고
 * grayscale 필터를 씌우면 탁해진다. 선으로 그린 아이콘은 currentColor를
 * 따라가므로 활성/비활성 색이 글자와 항상 일치한다.
 *
 * 모두 24x24 기준, 선 굵기 1.8로 통일했다.
 */

const PATHS = {
  home: <path d="M4 10.5 12 4l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5h-5v5H5a1 1 0 0 1-1-1z" />,

  sparkles: (
    <>
      <path d="M12 3.5 13.7 8 18 9.7 13.7 11.4 12 16l-1.7-4.6L6 9.7 10.3 8z" />
      <path d="M18.5 15.5l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z" />
    </>
  ),

  wallet: (
    <>
      <path d="M3 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
      <path d="M19 11h2v4h-2a2 2 0 0 1 0-4z" />
    </>
  ),

  ban: (
    <>
      <circle cx="12" cy="12" r="8" />
      <path d="M6.5 6.5l11 11" />
    </>
  ),

  // 룰렛. 원을 4등분한 바퀴 + 12시 방향 바늘.
  // sparkles를 재사용하면 사이드바에서 '오늘 뭐 먹지?'와 나란히 붙어 구분이 안 된다
  roulette: (
    <>
      <circle cx="12" cy="13" r="7.5" />
      <path d="M12 5.5v15M4.5 13h15" />
      <path d="M12 2.5l2 2.5h-4z" fill="currentColor" />
    </>
  ),

  clock: (
    <>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 7.5V12l3 2" />
    </>
  ),

  heart: (
    <path d="M12 19s-6.5-4.2-6.5-8.4A3.6 3.6 0 0 1 12 8.2a3.6 3.6 0 0 1 6.5 2.4C18.5 14.8 12 19 12 19z" />
  ),

  // 찜한 상태. 테두리만 있는 heart와 같은 형태를 채우기만 해서,
  // 켜고 끌 때 모양이 튀지 않고 색만 차오르는 것처럼 보인다
  'heart-fill': (
    <path
      d="M12 19s-6.5-4.2-6.5-8.4A3.6 3.6 0 0 1 12 8.2a3.6 3.6 0 0 1 6.5 2.4C18.5 14.8 12 19 12 19z"
      fill="currentColor"
    />
  ),

  chat: (
    <path d="M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H9l-5 4z" />
  ),

  user: (
    <>
      <circle cx="12" cy="8.5" r="3.5" />
      <path d="M5 20c0-3.6 3.1-5.5 7-5.5s7 1.9 7 5.5" />
    </>
  ),

  bell: (
    <>
      <path d="M6.5 10a5.5 5.5 0 0 1 11 0c0 4 1.5 5.5 1.5 5.5H5S6.5 14 6.5 10z" />
      <path d="M10 18.5a2 2 0 0 0 4 0" />
    </>
  ),

  search: (
    <>
      <circle cx="11" cy="11" r="6" />
      <path d="M15.5 15.5 20 20" />
    </>
  ),

  menu: <path d="M4 7h16M4 12h16M4 17h16" />,

  // 게임 패드. '메뉴 게임'은 세 가지 놀이를 묶은 메뉴라
  // 특정 놀이(룰렛·월드컵)의 모양을 쓰면 나머지를 대표하지 못한다
  gamepad: (
    <>
      <path d="M7.5 8h9a4.5 4.5 0 0 1 4.4 3.6l.7 4a2.6 2.6 0 0 1-4.7 1.9L15.6 16H8.4l-1.3 1.5a2.6 2.6 0 0 1-4.7-1.9l.7-4A4.5 4.5 0 0 1 7.5 8z" />
      <path d="M7 11.5v2.2M5.9 12.6h2.2" />
      <path d="M15.5 12h.01M17.5 14h.01" />
    </>
  ),

  // 냉장고. 손잡이 두 개와 가운데 칸막이로 읽힌다
  fridge: (
    <>
      <rect x="6" y="3" width="12" height="18" rx="2.4" />
      <path d="M6 10h12" />
      <path d="M9 6.5v2M9 12.5v2.5" />
    </>
  ),

  /*
   * 톱니바퀴. 큰 원 + 작은 구멍 + 짧고 뭉툭한 톱니 여덟 개.
   *
   * 처음엔 작은 원에 긴 선을 뻗어 그렸는데 바로 아래 sun 아이콘과 구별이 안 됐다.
   * 해는 "작은 원 + 긴 빛살", 톱니바퀴는 "큰 원 + 짧은 톱니 + 가운데 구멍"이라
   * 이 세 가지가 다르면 작은 크기에서도 헷갈리지 않는다.
   */
  gear: (
    <>
      <circle cx="12" cy="12" r="6.4" />
      <circle cx="12" cy="12" r="2.6" />
      <path d="M18.4 12h2.2M3.4 12h2.2M12 3.4v2.2M12 18.4v2.2" />
      <path d="M16.5 16.5l1.6 1.6M5.9 5.9l1.6 1.6M16.5 7.5l1.6-1.6M5.9 18.1l1.6-1.6" />
    </>
  ),

  sun: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2.5v2.2M12 19.3v2.2M21.5 12h-2.2M4.7 12H2.5M18.7 5.3l-1.6 1.6M6.9 17.1l-1.6 1.6M18.7 18.7l-1.6-1.6M6.9 6.9 5.3 5.3" />
    </>
  ),

  moon: <path d="M20 13.5A8 8 0 0 1 10.5 4a8 8 0 1 0 9.5 9.5z" />,

  // AI 도우미. 안테나 달린 로봇 얼굴
  robot: (
    <>
      <rect x="4" y="8" width="16" height="11" rx="3" />
      <path d="M12 4.5V8M9.5 13h.01M14.5 13h.01M9.5 16h5" />
      <circle cx="12" cy="3.6" r="1.1" />
    </>
  ),

  trophy: (
    <>
      <path d="M8 4h8v5a4 4 0 0 1-8 0z" />
      <path d="M8 5.5H5.5V7a3 3 0 0 0 3 3M16 5.5h2.5V7a3 3 0 0 1-3 3" />
      <path d="M12 13v3.5M9 20h6l-.6-3.5H9.6z" />
    </>
  ),

  star: (
    <path
      d="M12 4.5l2.3 4.7 5.2.8-3.8 3.6.9 5.1-4.6-2.4-4.6 2.4.9-5.1L4.5 10l5.2-.8z"
      fill="currentColor"
      stroke="none"
    />
  ),

  calendar: (
    <>
      <rect x="4" y="5.5" width="16" height="14.5" rx="2.4" />
      <path d="M4 10h16M8.5 3.5v4M15.5 3.5v4" />
    </>
  ),

  refresh: (
    <>
      <path d="M19.5 12a7.5 7.5 0 1 1-2.6-5.7" />
      <path d="M19.8 4.6v4.2h-4.2" />
    </>
  ),

  plus: <path d="M12 5.5v13M5.5 12h13" />,
}

export default function Icon({ name, size = 20, className }) {
  const path = PATHS[name]
  if (!path) return null

  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {path}
    </svg>
  )
}

/**
 * 서비스 로고 마크 — 주황 타일에 얹은 요리사 모자.
 *
 * 모자만 두면 흰 배경에서 형태가 흐려지고, 사이드바·상단바·앱 아이콘에서
 * 크기가 제각각일 때 눈에 띄지 않는다. 타일로 감싸면 어디에 놓든
 * 같은 덩어리로 읽힌다. 타일 색은 currentColor라 부모가 정한다.
 */
export function LogoMark({ size = 26 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="9" fill="currentColor" />
      {/* 모자 윗부분 — 원 세 개를 겹쳐 부풀린 형태를 만든다 */}
      <g fill="#fff">
        <circle cx="11" cy="12" r="5" />
        <circle cx="21" cy="12" r="5" />
        <circle cx="16" cy="10" r="5.6" />
        <path d="M10 14h12v6H10z" />
        {/* 챙 */}
        <rect x="9.5" y="19" width="13" height="5" rx="2" />
      </g>
      {/* 챙의 주름. 타일 색이 비쳐 보이게 해서 모자와 챙을 구분한다 */}
      <g stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" opacity="0.55">
        <path d="M13.5 20.4v2.2M16 20.4v2.2M18.5 20.4v2.2" />
      </g>
    </svg>
  )
}
