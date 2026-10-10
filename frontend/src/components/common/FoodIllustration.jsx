// 음식 그림 (SVG). 사진이 없는 음식에 대신 쓴다.
// 사진은 public/images/food 에 있고, 거기에 없는 음식만 이 그림으로 그린다.
// 서버가 주는 art 값으로 6가지 그림 중 하나를 고른다.
//
//   stew     붉은 찌개    김치찌개, 순두부찌개
//   soup     뽀얀 국물    소고기미역국, 북엇국
//   noodle   국밥, 면류   바지락칼국수, 콩국수
//   stirfry  볶음         제육볶음, 멸치볶음
//   pancake  전, 부침     김치부침개, 감자전
//   donburi  덮밥, 정식   소불고기, 고등어구이
//
// 왜 그림을 직접 그렸나
//
// 사진 100장을 다 준비할 수 없고, 사용자가 직접 검색한 이름으로도
// 카드가 그려진다. 사진이 없을 때 회색 네모를 두면 "안 불러와진 것"
// 처럼 보이는데, 그림이 있으면 그 자리가 비어 보이지 않는다.
//
// 여섯 가지로 줄인 이유 — 음식마다 그리면 100장이다. "국물이 붉은지
// 뽀얀지, 면인지 볶음인지" 정도만 맞으면 카드로는 충분하다.
//
// SVG 로 그린 이유 — 글자라서 파일을 따로 두지 않아도 되고,
// 크기를 늘려도 흐려지지 않는다. 카드(lg)와 표(sm)에 같은 그림을 쓴다.

// 색을 한 곳에 모아둔다. 그림 여섯 개가 같이 쓰는 값이라,
// 각자 적으면 같은 국물 색이 미묘하게 달라진다
const C = {
  broth: '#d9482a',       // 붉은 국물
  brothDark: '#b93a20',
  cream: '#f2e3c9',       // 뽀얀 국물
  creamDark: '#e3cfae',
  clay: '#8d5a3b',        // 뚝배기
  clayDark: '#6f4530',
  bowl: '#ffffff',
  bowlDark: '#e9e3da',
  pan: '#3b3532',
  panDark: '#2a2523',
  meat: '#c2622f',
  meatDark: '#a34e22',
  golden: '#e2a44a',
  goldenDark: '#c8862f',
  green: '#6f9c4f',
  white: '#fffaf4',
  shadow: '#e0cbb8',
}

// 그릇 아래 그림자. 모든 그림이 같이 쓴다.
//
// 작은 조각을 함수로 빼둔 이유 — 여섯 그림이 같이 쓴다. 각자 적으면
// 그림자 위치가 조금씩 달라져서, 카드를 나란히 놓으면 들쭉날쭉해 보인다
function Ground() {
  return <ellipse cx="120" cy="132" rx="76" ry="9" fill={C.shadow} opacity="0.55" />
}

// 김. 그릇 위로 올라가는 흰 곡선 3줄.
//
// y 를 받는 이유 — 그릇 높이가 그림마다 다르다. 뚝배기는 높고
// 접시는 낮아서, 같은 자리에 김을 두면 그릇 속에서 올라오는 것처럼
// 보이지 않는다
function Steam({ y = 28 }) {
  return (
    <g stroke={C.white} strokeWidth="5" strokeLinecap="round" fill="none" opacity="0.75">
      <path d={`M96 ${y + 16}c0-9 9-9 9-18s-6-10-6-10`} />
      <path d={`M120 ${y + 10}c0-11 10-11 10-21s-7-12-7-12`} />
      <path d={`M146 ${y + 16}c0-9 9-9 9-18s-6-10-6-10`} />
    </g>
  )
}

function Stew() {
  return (
    <>
      <Steam y={26} />
      <Ground />
      {/* 뚝배기 손잡이 */}
      <circle cx="42" cy="84" r="11" fill={C.clayDark} />
      <circle cx="198" cy="84" r="11" fill={C.clayDark} />
      {/* 몸통 */}
      <path d="M52 76h136c-4 34-24 52-68 52S56 110 52 76z" fill={C.clay} />
      <path d="M52 76h136c-1 9-3 17-6 24H58c-3-7-5-15-6-24z" fill={C.clayDark} opacity="0.35" />
      {/* 국물 */}
      <ellipse cx="120" cy="76" rx="68" ry="13" fill={C.clayDark} />
      <ellipse cx="120" cy="75" rx="61" ry="10" fill={C.broth} />
      {/* 두부 */}
      <rect x="94" y="66" width="24" height="13" rx="3" fill={C.white} />
      <rect x="126" y="71" width="20" height="11" rx="3" fill={C.white} opacity="0.92" />
      {/* 대파 */}
      <ellipse cx="88" cy="79" rx="7" ry="4" fill={C.green} />
      <ellipse cx="152" cy="71" rx="6" ry="3.5" fill={C.green} />
      <ellipse cx="118" cy="82" rx="5" ry="3" fill={C.brothDark} />
    </>
  )
}

function Soup() {
  return (
    <>
      <Steam y={26} />
      <Ground />
      {/* 그릇 */}
      <path d="M50 74h140c-5 35-27 54-70 54S55 109 50 74z" fill={C.bowl} />
      <path d="M50 74h140c-2 10-5 19-9 26H59c-4-7-7-16-9-26z" fill={C.bowlDark} opacity="0.5" />
      <ellipse cx="120" cy="74" rx="70" ry="13" fill={C.bowlDark} />
      {/* 국물. 그릇이 흰색이라 국물을 충분히 진하게 해야 형태가 읽힌다 */}
      <ellipse cx="120" cy="73" rx="63" ry="10" fill={C.creamDark} />
      <ellipse cx="120" cy="74" rx="58" ry="8" fill={C.cream} />
      {/* 수제비 조각. 흰 반죽이 국물에 묻히지 않도록 테두리를 준다 */}
      <g fill={C.white} stroke={C.creamDark} strokeWidth="2">
        <path d="M90 68c10-5 20-2 21 3s-7 10-16 9-14-8-5-12z" />
        <path d="M129 70c9-4 18-1 18 4s-9 9-17 7-8-8-1-11z" />
        <path d="M107 78c8-3 14 0 14 4s-7 6-13 5-8-7-1-9z" />
      </g>
      {/* 고명 */}
      <ellipse cx="151" cy="66" rx="8" ry="4" fill={C.green} />
      <ellipse cx="82" cy="76" rx="6" ry="3.5" fill={C.green} />
      <ellipse cx="120" cy="65" rx="5" ry="3" fill={C.green} opacity="0.8" />
      {/* 받침 */}
      <rect x="96" y="126" width="48" height="7" rx="3.5" fill={C.bowlDark} />
    </>
  )
}

function Noodle() {
  return (
    <>
      <Steam y={26} />
      <Ground />
      <path d="M50 74h140c-5 35-27 54-70 54S55 109 50 74z" fill={C.bowl} />
      <path d="M50 74h140c-2 10-5 19-9 26H59c-4-7-7-16-9-26z" fill={C.bowlDark} opacity="0.5" />
      <ellipse cx="120" cy="74" rx="70" ry="13" fill={C.bowlDark} />
      <ellipse cx="120" cy="73" rx="63" ry="10" fill={C.creamDark} />
      <ellipse cx="120" cy="74" rx="58" ry="8" fill={C.cream} />
      {/* 콩나물 가닥. 가늘면 축소 시 사라지므로 굵게 그린다 */}
      <g stroke={C.golden} strokeWidth="5" strokeLinecap="round" fill="none">
        <path d="M82 72c12-7 24-7 36 0s24 7 36 0" />
        <path d="M88 80c11-6 21-6 32 0s21 6 32 0" />
      </g>
      {/* 계란 */}
      <ellipse cx="103" cy="69" rx="15" ry="9" fill={C.white} stroke={C.creamDark} strokeWidth="1.5" />
      <ellipse cx="103" cy="69" rx="6.5" ry="4.5" fill={C.golden} />
      {/* 파 */}
      <ellipse cx="150" cy="67" rx="8" ry="4" fill={C.green} />
      <ellipse cx="136" cy="80" rx="6" ry="3.5" fill={C.green} />
      <rect x="96" y="126" width="48" height="7" rx="3.5" fill={C.bowlDark} />
    </>
  )
}

function Stirfry() {
  return (
    <>
      <Steam y={30} />
      <Ground />
      {/* 손잡이 */}
      <rect x="186" y="80" width="46" height="10" rx="5" fill={C.panDark} />
      {/* 팬 */}
      <path d="M46 78h148c-4 32-30 50-74 50S50 110 46 78z" fill={C.pan} />
      <ellipse cx="120" cy="78" rx="74" ry="14" fill={C.panDark} />
      <ellipse cx="120" cy="77" rx="66" ry="11" fill="#4a423e" />
      {/* 양념 볶음 재료 */}
      <g fill={C.meat}>
        <rect x="86" y="64" width="26" height="15" rx="6" transform="rotate(-12 99 71)" />
        <rect x="118" y="70" width="28" height="15" rx="6" transform="rotate(9 132 77)" />
        <rect x="98" y="80" width="24" height="13" rx="6" transform="rotate(-4 110 86)" />
      </g>
      <g fill={C.meatDark} opacity="0.65">
        <rect x="140" y="62" width="20" height="12" rx="5" transform="rotate(-16 150 68)" />
        <rect x="72" y="76" width="18" height="11" rx="5" transform="rotate(7 81 81)" />
      </g>
      {/* 깻잎 · 참깨 */}
      <ellipse cx="152" cy="80" rx="9" ry="5" fill={C.green} />
      <ellipse cx="80" cy="68" rx="7" ry="4" fill={C.green} />
      <g fill={C.white} opacity="0.85">
        <circle cx="110" cy="66" r="1.8" />
        <circle cx="132" cy="84" r="1.8" />
        <circle cx="94" cy="86" r="1.8" />
      </g>
    </>
  )
}

function Pancake() {
  return (
    <>
      <Ground />
      <rect x="186" y="82" width="46" height="10" rx="5" fill={C.panDark} />
      <path d="M46 80h148c-4 31-30 48-74 48S50 111 46 80z" fill={C.pan} />
      <ellipse cx="120" cy="80" rx="74" ry="14" fill={C.panDark} />
      {/* 전 */}
      <ellipse cx="120" cy="78" rx="60" ry="21" fill={C.golden} />
      <ellipse cx="120" cy="75" rx="60" ry="21" fill={C.goldenDark} opacity="0.35" />
      <ellipse cx="120" cy="74" rx="56" ry="19" fill={C.golden} />
      {/* 김치 · 파 조각 */}
      <g fill={C.broth} opacity="0.9">
        <ellipse cx="100" cy="70" rx="11" ry="5" transform="rotate(-14 100 70)" />
        <ellipse cx="138" cy="78" rx="10" ry="4.5" transform="rotate(10 138 78)" />
        <ellipse cx="112" cy="82" rx="8" ry="4" transform="rotate(-6 112 82)" />
      </g>
      <ellipse cx="146" cy="67" rx="8" ry="3.5" fill={C.green} />
      <ellipse cx="88" cy="80" rx="7" ry="3" fill={C.green} />
      {/* 뒤집개 */}
      <g opacity="0.9">
        <rect x="150" y="34" width="9" height="34" rx="4.5" fill={C.clayDark} transform="rotate(24 154 51)" />
      </g>
    </>
  )
}

function Donburi() {
  return (
    <>
      <Ground />
      {/*
        접시를 흰색으로 두면 흰 밥이 묻혀서 형태가 안 보인다.
        접시는 살짝 어두운 베이지로, 밥은 흰색으로 대비를 준다.
      */}
      <ellipse cx="120" cy="100" rx="84" ry="25" fill="#d9d2c8" />
      <ellipse cx="120" cy="96" rx="84" ry="25" fill={C.bowlDark} />
      <ellipse cx="120" cy="94" rx="72" ry="20" fill="#f3eee7" />

      {/* 밥 */}
      <path d="M60 92c0-16 14-27 32-27s32 11 32 27z" fill={C.white} stroke="#ddd5c9" strokeWidth="2" />
      {/* 밥알 질감 */}
      <g fill="#e6ded1">
        <ellipse cx="80" cy="80" rx="4" ry="2.5" />
        <ellipse cx="97" cy="74" rx="4" ry="2.5" />
        <ellipse cx="106" cy="84" rx="4" ry="2.5" />
      </g>

      {/* 돈까스 슬라이스 3조각 */}
      <g stroke={C.goldenDark} strokeWidth="2">
        <rect x="132" y="62" width="19" height="32" rx="6" fill={C.golden} transform="rotate(12 141 78)" />
        <rect x="149" y="66" width="19" height="32" rx="6" fill={C.golden} transform="rotate(12 158 82)" />
        <rect x="166" y="70" width="19" height="32" rx="6" fill={C.golden} transform="rotate(12 175 86)" />
      </g>
      {/* 단면(고기) */}
      <g fill="#f0dfc4">
        <rect x="136" y="70" width="11" height="16" rx="3" transform="rotate(12 141 78)" />
        <rect x="153" y="74" width="11" height="16" rx="3" transform="rotate(12 158 82)" />
        <rect x="170" y="78" width="11" height="16" rx="3" transform="rotate(12 175 86)" />
      </g>

      {/* 곁들임 채소 */}
      <ellipse cx="86" cy="66" rx="13" ry="7" fill={C.green} />
      <ellipse cx="86" cy="64" rx="9" ry="4" fill="#84b062" />
      <circle cx="108" cy="62" r="6" fill={C.broth} />
    </>
  )
}

// 이름 -> 그림 함수. 서버가 주는 art 값으로 찾는다.
//
// 함수를 값으로 담아둔다(Stew() 가 아니라 Stew). 아래에서 <Art /> 로
// 쓰려면 함수 자체여야 한다
const ART = {
  stew: Stew,
  soup: Soup,
  noodle: Noodle,
  stirfry: Stirfry,
  pancake: Pancake,
  donburi: Donburi,
}

export default function FoodIllustration({ art, className }) {
  // 모르는 값이 오면 기본 그림(Soup)을 쓴다
  //
  // art 가 아예 없는 경우도 있다(카탈로그에 없는 음식). null 을
  // 돌려주면 그 자리가 비므로, 무난한 그림 하나로 떨어뜨린다.
  //
  // 변수 이름을 대문자로 시작하는 이유 — 리액트는 소문자로 시작하는
  // 태그를 HTML 로 본다. <art /> 로 쓰면 그런 HTML 태그를 찾다가
  // 아무것도 안 그린다
  const Art = ART[art] || Soup

  // width · height 를 안 적는다. 쓰는 쪽에서 CSS 로 정한다 -
  // 카드 · 목록 · 표의 크기가 달라서, 여기서 박으면 맞출 수 없다.
  //
  // viewBox 가 가로로 긴 이유(240x150) — 카드 사진 자리가
  // 가로로 긴 모양이다.
  //
  // preserveAspectRatio="xMidYMid meet" 은 "비율을 지키면서
  // 가운데에 넣어라" 는 뜻이다. 안 주면 칸 모양에 맞춰 늘어나서
  // 그릇이 찌그러진다
  return (
    <svg
      className={className}
      viewBox="0 0 240 150"
      preserveAspectRatio="xMidYMid meet"
      aria-hidden="true"
    >
      <Art />
    </svg>
  )
}
