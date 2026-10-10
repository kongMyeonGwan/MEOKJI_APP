// 아이콘 모음 (SVG).
// 쓰는 법: <Icon name="heart" size={20} />
// 글자 색(color)을 바꾸면 아이콘 색도 같이 바뀐다.
//
// 왜 아이콘 라이브러리를 안 쓰나
//
// 쓰는 아이콘이 열 몇 개뿐이다. 라이브러리를 넣으면 수천 개가 같이
// 들어오고(쓰지 않는 것까지), 모양도 우리 화면에 맞춰 고칠 수 없다.
//
// 왜 이미지(png)가 아니라 SVG 인가
//
// 아래 stroke="currentColor" 때문이다. 글자 색을 따라가므로
// 다크 모드에서 알아서 바뀌고, 눌린 메뉴의 강조색도 CSS 만으로 된다.
// png 면 테마마다 두 벌을 두고 골라 써야 한다.
//
// 모양은 전부 24x24 칸 안에 그려져 있다(viewBox). 그래서 size 만
// 바꿔도 비율이 유지된다.
//
// 아이콘을 더할 때 — 아래 PATHS 에 이름과 <path> 를 적으면 끝이다.
// 다른 파일은 안 고친다.

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

  // 찜 목록의 보기 전환 버튼 (격자 / 목록)
  grid: (
    <>
      <rect x="4" y="4" width="7" height="7" rx="1.5" />
      <rect x="13" y="4" width="7" height="7" rx="1.5" />
      <rect x="4" y="13" width="7" height="7" rx="1.5" />
      <rect x="13" y="13" width="7" height="7" rx="1.5" />
    </>
  ),

  list: (
    <>
      <path d="M8 6.5h12M8 12h12M8 17.5h12" />
      <path d="M4 6.5h.01M4 12h.01M4 17.5h.01" />
    </>
  ),

  bookmark: <path d="M7 4.5h10a1 1 0 0 1 1 1V20l-6-3.5L6 20V5.5a1 1 0 0 1 1-1z" />,

  // 저장한 상태. heart / heart-fill 과 같은 방식으로, 모양은 그대로 두고 색만 채운다
  'bookmark-fill': (
    <path d="M7 4.5h10a1 1 0 0 1 1 1V20l-6-3.5L6 20V5.5a1 1 0 0 1 1-1z" fill="currentColor" />
  ),

  // 인기 레시피. 찜(heart)과 헷갈리지 않게 불꽃으로 둔다
  fire: (
    <path d="M12 3.5c2.8 2.6 4.5 5 4.5 7.6a4.5 4.5 0 0 1-9 0c0-1.3.5-2.5 1.5-3.6.2 1.2.8 2 1.7 2.3-.5-2.2-.2-4.2 1.3-6.3z" />
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
  // 없는 이름이면 아무것도 안 그린다. 이름에 오타가 나면 그 자리만
  // 비는데, 에러가 안 나서 알아채기 어렵다 - 대신 화면이 깨지지는 않는다
  if (!path) {
    return null
  }

  // viewBox 는 "그림이 그려진 칸의 크기" 다. 아래 width · height 와
  // 달라도 되고, 다르면 늘려서 맞춘다 - 그래서 size 만 바꾸면
  // 비율이 유지된다.
  //
  // fill="none" + stroke : 속을 채우지 않고 선만 그린다.
  // 선 아이콘이라 글자와 같이 놓았을 때 무게가 비슷하다.
  //
  // currentColor 는 "지금 글자 색" 이다. 이것 덕분에 CSS 로
  // color 만 바꾸면 아이콘 색도 따라온다.
  //
  // strokeLinecap · strokeLinejoin 을 round 로 두면 선 끝과
  // 꺾이는 자리가 둥글어진다. 안 주면 뾰족해서 작은 크기에서 거칠다.
  //
  // aria-hidden 은 "읽어주지 말라" 는 표시다. 아이콘 옆에 글자가
  // 같이 있으므로, 읽으면 같은 말이 두 번 들린다
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

// 로고. 주황 네모 위에 요리사 모자
//
// 위 Icon 과 달리 export default 가 아니다. 한 파일에서 둘을
// 내보내는 모양이고, 쓰는 쪽은 이렇게 가져온다
//   import Icon, { LogoMark } from './Icon'
//
// 같은 파일에 둔 이유 — 둘 다 SVG 그림이고, 로고는 한 군데(상단바 ·
// 사이드바)에서만 쓴다. 파일을 따로 만들 만큼 크지 않다.
//
// Icon 과 달리 fill 로 속을 채운다. 로고는 선 그림이 아니라
// 색이 찬 모양이어야 눈에 띈다
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
