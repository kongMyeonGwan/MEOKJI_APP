import { useEffect, useRef } from 'react'

// 폭죽(색종이) 효과. 화면 전체를 덮는 투명한 캔버스에 색종이를 터뜨린다.
//
//   <Confetti fire={count} />
//
// fire 값이 바뀔 때마다 한 번 터진다 (0 이면 안 터진다).
// 화면 아래 양쪽 모서리에서 위로 쏘아 올리고, 중력으로 떨어지며 사라진다.
// 클릭을 막지 않도록 pointer-events 를 끈다. 다 떨어지면 캔버스를 비운다.
//
// 기기에서 '동작 줄이기'를 켰으면 터뜨리지 않는다.

const COLORS = ['#e8481c', '#f6c445', '#7fae4e', '#6b4ddb', '#f97c3c', '#2fa4e7', '#ff6fa5']
const PIECES = 160 // 한쪽에 80개씩
const DURATION = 3200 // ms

export default function Confetti({ fire }) {
  const canvasRef = useRef(null)

  useEffect(() => {
    if (!fire) {
      return
    }
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return
    }

    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    const ratio = window.devicePixelRatio || 1
    const width = window.innerWidth
    const height = window.innerHeight
    canvas.width = width * ratio
    canvas.height = height * ratio
    ctx.scale(ratio, ratio)

    // 색종이 하나: 위치, 속도, 회전, 색, 크기
    const pieces = []
    for (let i = 0; i < PIECES; i++) {
      const fromLeft = i % 2 === 0
      // 왼쪽은 오른쪽 위로, 오른쪽은 왼쪽 위로 쏜다 (각도는 위쪽 기준 15~50도)
      const angle = ((15 + Math.random() * 35) * Math.PI) / 180
      const speed = 11 + Math.random() * 9
      pieces.push({
        x: fromLeft ? 0 : width,
        y: height * 0.85,
        vx: Math.sin(angle) * speed * (fromLeft ? 1 : -1),
        vy: -Math.cos(angle) * speed * (height / 800),
        rotation: Math.random() * Math.PI,
        spin: (Math.random() - 0.5) * 0.3,
        color: COLORS[i % COLORS.length],
        w: 6 + Math.random() * 6,
        h: 8 + Math.random() * 8,
      })
    }

    let frame = 0
    const start = performance.now()

    const draw = (now) => {
      const elapsed = now - start
      ctx.clearRect(0, 0, width, height)

      // 끝나기 0.8초 전부터 서서히 투명해진다
      ctx.globalAlpha = Math.max(0, Math.min(1, (DURATION - elapsed) / 800))

      for (const p of pieces) {
        p.vy += 0.32 // 중력
        p.vx *= 0.985 // 공기 저항
        p.vy *= 0.985
        p.x += p.vx
        p.y += p.vy
        p.rotation += p.spin

        ctx.save()
        ctx.translate(p.x, p.y)
        ctx.rotate(p.rotation)
        // 세로 폭을 cos 로 줄였다 늘려서 팔랑거리는 느낌을 준다
        ctx.scale(1, Math.cos(p.rotation * 3))
        ctx.fillStyle = p.color
        ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h)
        ctx.restore()
      }

      if (elapsed < DURATION) {
        frame = requestAnimationFrame(draw)
      } else {
        ctx.clearRect(0, 0, width, height)
      }
    }
    frame = requestAnimationFrame(draw)

    // 다시 터뜨리거나 화면을 떠나면 하던 것을 멈춘다
    return () => {
      cancelAnimationFrame(frame)
      ctx.setTransform(1, 0, 0, 1, 0, 0)
      ctx.clearRect(0, 0, canvas.width, canvas.height)
    }
  }, [fire])

  return <canvas ref={canvasRef} className="confetti" aria-hidden="true" />
}
