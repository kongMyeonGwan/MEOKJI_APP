import { Link } from 'react-router-dom'
import Icon from '../components/common/Icon'
import FoodThumb from '../components/common/FoodThumb'
import { useLikes } from '../contexts/LikesContext'
import '../styles/pages/ListPage.css'
import '../styles/components/recommendation/LikedList.css'

/**
 * 찜한 레시피 — 내가 하트를 누른 목록.
 *
 * 인기 레시피(/popular)와 헷갈리기 쉬운데 주인이 다르다.
 *   찜한 레시피  내가 담아둔 것. 남들의 좋아요 수와 무관하다
 *   인기 레시피  남들이 많이 누른 것
 *
 * 순서는 최근에 찜한 순이다(LikesContext가 앞에 넣는다).
 * 좋아요 수로 정렬하면 내가 방금 담은 게 목록 아래로 사라진다.
 *
 * 왜 카드가 아니라 이름 목록인가
 * ------------------------------
 * 찜은 이 브라우저에 **이름만** 저장한다(LikesContext).
 * 조리시간·가격 같은 걸 같이 보여주려면 찜한 개수만큼 AI 호출이 필요한데,
 * 목록을 여는 데 그만한 시간을 쓸 이유가 없다. 눌러서 들어가면 그때 만든다.
 */
export default function LikedPage() {
  const { names, count, toggle } = useLikes()

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">찜한 레시피</h2>
        {count > 0 && <span className="muted section-more">{count}건</span>}
      </div>

      {count === 0 ? (
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
      ) : (
        <div className="card">
          <ul className="liked">
            {names.map((name) => (
              <li key={name}>
                <Link
                  to={`/recipe/${encodeURIComponent(name)}`}
                  className="liked__row"
                >
                  {/* art 를 모르니 기본 그림이 그려진다. 이름은 읽어준다 */}
                  <FoodThumb item={{ food_name: name }} size="md" />

                  <span className="liked__body">
                    <span className="liked__name">{name}</span>
                    <span className="liked__meta muted">
                      눌러서 레시피를 만들어 보세요
                    </span>
                  </span>
                </Link>

                {/*
                  하트를 목록 안에 둔다. 레시피를 열어야만 찜을 뺄 수 있으면
                  정리하는 데 화면 이동이 매번 필요하다.
                */}
                <button
                  type="button"
                  className="liked__unlike"
                  onClick={() => toggle(name)}
                  aria-label={`${name} 찜 해제`}
                  title="찜 해제"
                >
                  <Icon name="heart-fill" size={18} />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
