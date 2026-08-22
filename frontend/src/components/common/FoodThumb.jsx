import FoodIllustration from './FoodIllustration'
import '../../styles/components/common/FoodThumb.css'

/**
 * 음식 썸네일.
 *
 * 아직 실제 사진이 없어서 SVG 일러스트로 대체한다.
 * mocks의 image_url 이 채워지면 그때부터 사진을 우선 사용하므로,
 * 나중에 이미지가 생겨도 이 컴포넌트만 알아서 바뀐다.
 *
 * size: 'lg'(카드 상단) | 'md'(목록) | 'sm'(표)
 */
export default function FoodThumb({ item, size = 'md', alt }) {
  const label = alt ?? item?.food_name ?? ''

  if (item?.image_url) {
    return (
      <img
        className={`thumb thumb--${size}`}
        src={item.image_url}
        alt={label}
        loading="lazy"
      />
    )
  }

  return (
    <div className={`thumb thumb--${size}`} role="img" aria-label={label}>
      <FoodIllustration art={item?.art} className="thumb__art" />
    </div>
  )
}
