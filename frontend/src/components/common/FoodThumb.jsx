import FoodIllustration from './FoodIllustration'
import './FoodThumb.css'

// 음식 썸네일. 사진(image_url)이 있으면 사진을, 없으면 SVG 그림을 보여준다.
// size 는 'lg'(카드), 'md'(목록), 'sm'(표) 중 하나
export default function FoodThumb({ item, size = 'md' }) {
  const className = `thumb thumb--${size}`

  if (item.image_url) {
    return <img className={className} src={item.image_url} alt={item.food_name} />
  }

  return (
    <div className={className}>
      <FoodIllustration art={item.art} className="thumb__art" />
    </div>
  )
}
