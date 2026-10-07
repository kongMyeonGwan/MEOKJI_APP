import { NavLink } from 'react-router-dom'
import Icon from '../../../components/common/Icon'
import { SAMPLE_NOTICES } from '../../../data/sampleBoard'

// 커뮤니티 게시판 셋을 오가는 탭.
// 세 목록 화면(레시피 추천 · 공지사항 · 자유게시판)이 같이 쓴다. 그래서
// 어느 한 게시판 폴더가 아니라 community/components/ 에 둔다.
//
// 사이드바에는 '커뮤니티' 하나만 있고, 게시판 사이는 이 탭으로 옮겨 다닌다.
//
// TODO(게시판 담당): 공지 개수는 지금 가데이터 길이다. 목록 API 가 붙으면
//   서버가 주는 전체 개수(total)로 바꾼다.
const TABS = [
  { to: '/notice', label: '공지사항', icon: 'bell', count: SAMPLE_NOTICES.length },
  // { to: '/community', label: '레시피 추천', icon: 'sparkles' },
  { to: '/free', label: '자유게시판', icon: 'chat' },
]

export default function CommunityTabs() {
  return (
    <div className="community__tabs">
      {TABS.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          // 지금 주소와 같은 탭에 is-active 가 붙는다 (NavLink 가 알려준다)
          end
          className={({ isActive }) => (isActive ? 'community__tab is-active' : 'community__tab')}
        >
          <Icon name={tab.icon} size={16} />
          {tab.label}
          {/* count > 0 으로 쓴다. tab.count 만 쓰면 0 일 때 숫자 0 이 화면에 찍힌다 */}
          {tab.count > 0 && <span className="community__tab-count">{tab.count}</span>}
        </NavLink>
      ))}
    </div>
  )
}
