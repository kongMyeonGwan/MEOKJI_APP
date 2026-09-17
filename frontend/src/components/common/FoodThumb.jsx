import FoodIllustration from './FoodIllustration'
import '../../styles/components/common/FoodThumb.css'

// 음식 썸네일.
// 아직 진짜 사진이 없어서 SVG 그림으로 대신한다.
// 나중에 서버가 image_url 을 채워주면 그때부터 사진을 먼저 쓴다.
//
// size 는 'lg'(카드 위), 'md'(목록), 'sm'(표) 중 하나
export default function FoodThumb({ item, size }) {
  let thumbSize = size
  if (!thumbSize) {
    thumbSize = 'md'
  }

  let label = ''
  if (item && item.food_name) {
    label = item.food_name
  }

  const className = 'thumb thumb--' + thumbSize

  if (item && item.image_url) {
    return (
      <img
        className={className}
        src={item.image_url}
        alt={label}
        loading="lazy"
      />
    )
  }

  let art = null
  if (item) {
    art = item.art
  }

  return (
    <div className={className}>
      <FoodIllustration art={art} className="thumb__art" />
    </div>
  )
}
