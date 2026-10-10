import { useState } from 'react'
import FoodIllustration from './FoodIllustration'
import { foodImageUrl } from '../../utils/foodImage'

// 음식 썸네일.
//   1. 서버가 준 사진 주소(image_url)
//   2. 없으면 음식 이름으로 만든 주소 (/images/food/김치찌개.png)
//   3. 그 사진도 없으면 SVG 그림(FoodIllustration)
//
// size 는 'lg'(카드), 'md'(목록), 'sm'(표) 중 하나
export default function FoodThumb({ item, size = 'md' }) {
  // 사진을 못 불러온 음식 이름. 이름으로 담아두는 이유는 월드컵처럼 같은 자리에
  // 다른 음식이 들어오는 화면에서, 앞 음식의 실패가 남지 않게 하려는 것이다.
  const [brokenName, setBrokenName] = useState('')

  const className = `thumb thumb--${size}`
  const src = item.image_url ? item.image_url : foodImageUrl(item.food_name)

  if (src && brokenName !== item.food_name) {
    return (
      <img
        className={className}
        src={src}
        alt={item.food_name}
        onError={() => setBrokenName(item.food_name)}
      />
    )
  }

  return (
    <div className={className}>
      <FoodIllustration art={item.art} className="thumb__art" />
    </div>
  )
}
