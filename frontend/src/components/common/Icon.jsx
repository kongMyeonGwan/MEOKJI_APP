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

  clock: (
    <>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 7.5V12l3 2" />
    </>
  ),

  heart: (
    <path d="M12 19s-6.5-4.2-6.5-8.4A3.6 3.6 0 0 1 12 8.2a3.6 3.6 0 0 1 6.5 2.4C18.5 14.8 12 19 12 19z" />
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

/** 서비스 로고 마크 (밥그릇). */
export function LogoMark({ size = 26 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <path d="M4 15h24c-1 8-6.5 13-12 13S5 23 4 15z" fill="currentColor" />
      <ellipse cx="16" cy="15" rx="12" ry="2.6" fill="currentColor" opacity="0.55" />
      <g stroke="currentColor" strokeWidth="2" strokeLinecap="round" fill="none">
        <path d="M11 10c0-2 2-2.4 2-4.4S11.7 4 11.7 4" />
        <path d="M16 9c0-2.4 2.2-2.8 2.2-5S16.6 2 16.6 2" />
        <path d="M21 10c0-2 2-2.4 2-4.4S21.7 4 21.7 4" />
      </g>
    </svg>
  )
}
