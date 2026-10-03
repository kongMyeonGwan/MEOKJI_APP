import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../../components/common/Icon'
import FoodThumb from '../../components/common/FoodThumb'
import Loading from '../../components/common/Loading'
import { getLikes, toggleLike } from '../../api/likes'

// 찜한 레시피 목록. 서버에는 음식 이름만 저장돼 있어서 이름 목록으로 보여준다.
export default function LikedPage() {
  const [names, setNames] = useState([])
  const [loading, setLoading] = useState(true)

  // 찜 목록을 서버에서 받아온다
  const loadLikes = async () => {
    try {
      const data = await getLikes()
      setNames(data.names)
    } catch (err) {
      alert('찜 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadLikes()
  }, [])

  // 하트를 누르면 찜을 풀고 목록을 다시 받아온다
  const handleUnlike = async (name) => {
    try {
      await toggleLike(name)
      loadLikes()
    } catch (err) {
      alert('찜을 풀지 못했습니다.')
    }
  }

  return (
    <div className="list-page">
      <div className="section-head">
        <h2 className="section-title">찜한 레시피</h2>
        {names.length > 0 && <span className="muted section-more">{names.length}건</span>}
      </div>

      {loading && <Loading message="찜한 레시피를 불러오는 중..." />}

      {!loading && names.length === 0 && (
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

      {!loading && names.length > 0 && (
        <div className="card">
          <ul className="liked">
            {names.map((name) => (
              <li key={name}>
                <Link to={`/recipe/${name}`} className="liked__row">
                  <FoodThumb item={{ food_name: name }} size="md" />
                  <span className="liked__body">
                    <span className="liked__name">{name}</span>
                    <span className="liked__meta muted">눌러서 레시피를 만들어 보세요</span>
                  </span>
                </Link>

                <button type="button" className="liked__unlike" onClick={() => handleUnlike(name)} title="찜 해제">
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
