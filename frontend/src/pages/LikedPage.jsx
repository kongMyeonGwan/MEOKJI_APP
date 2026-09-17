import { Link } from 'react-router-dom'
import Icon from '../components/common/Icon'
import FoodThumb from '../components/common/FoodThumb'
import Loading from '../components/common/Loading'
import { useLikes } from '../contexts/LikesContext'
import '../styles/pages/ListPage.css'
import '../styles/components/recommendation/LikedList.css'

// 찜한 레시피. 내가 하트를 누른 목록이다.
//
// 인기 레시피(/popular) 랑 헷갈리기 쉬운데 주인이 다르다.
//   찜한 레시피 - 내가 담아둔 것
//   인기 레시피 - 남들이 많이 누른 것
//
// 순서는 최근에 찜한 순이다. 좋아요 수로 정렬하면 방금 담은 게 아래로 사라진다.
//
// 카드가 아니라 이름 목록인 이유는 찜을 서버에 이름만 저장하기 때문이다.
// 조리시간이나 가격까지 보여주려면 찜한 개수만큼 AI 를 불러야 한다.
export default function LikedPage() {
  const { names, count, toggle, ready } = useLikes()

  // 서버에서 받아오기 전에는 "없어요" 를 안 띄운다.
  // 안 그러면 화면이 열리자마자 빈 목록 문구가 깜빡였다가 목록으로 바뀐다.
  if (!ready) {
    return (
      <div className="list-page">
        <div className="section-head">
          <h2 className="section-title">찜한 레시피</h2>
        </div>
        <Loading message="찜한 레시피를 불러오는 중..." />
      </div>
    )
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">찜한 레시피</h2>
        {count > 0 && <span className="muted section-more">{count}건</span>}
      </div>

      {count === 0 && (
        <p className="empty">
          아직 찜한 레시피가 없어요.
          <br />
          카드 오른쪽 위의 하트를 누르면 여기에 모입니다.
          <br />
          <br />
          <Link to="/recommend" className="btn btn--outline btn--sm">
            추천 메뉴 보러 가기
          </Link>
        </p>
      )}

      {count > 0 && (
        <div className="card">
          <ul className="liked">
            {names.map(function (name) {
              return (
                <li key={name}>
                  <Link
                    to={'/recipe/' + encodeURIComponent(name)}
                    className="liked__row"
                  >
                    {/* 그림 종류를 모르니까 기본 그림이 그려진다 */}
                    <FoodThumb item={{ food_name: name }} size="md" />

                    <span className="liked__body">
                      <span className="liked__name">{name}</span>
                      <span className="liked__meta muted">
                        눌러서 레시피를 만들어 보세요
                      </span>
                    </span>
                  </Link>

                  {/* 하트를 목록 안에 뒀다. 레시피를 열어야만 찜을 뺄 수 있으면
                      정리할 때마다 화면을 옮겨야 한다. */}
                  <button
                    type="button"
                    className="liked__unlike"
                    onClick={() => toggle(name)}
                    aria-label={name + ' 찜 해제'}
                    title="찜 해제"
                  >
                    <Icon name="heart-fill" size={18} />
                  </button>
                </li>
              )
            })}
          </ul>
        </div>
      )}
    </div>
  )
}
