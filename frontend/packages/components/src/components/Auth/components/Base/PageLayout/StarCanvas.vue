<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(defineProps<{
  starCount?: number
}>(), {
  starCount: 200,
})

const starCanvasRef = ref<HTMLCanvasElement>()
let ctx: CanvasRenderingContext2D
let width = 0
let height = 0
let stars: Star[] = []
let rafId = 0

class Star {
  x = 0
  y = 0
  speed = 0
  size = 0
  opacity = 0
  currentOpacity = 0

  constructor() {
    this.init(true)
  }

  init(firstRun = false) {
    this.x = Math.random() * width
    this.y = firstRun ? Math.random() * height : height + Math.random() * 20

    const depth = Math.random()
    this.speed = depth * 0.1 + 0.1
    this.size = depth * 0.5 + 0.4
    this.opacity = depth * 0.6 + 0.6
  }

  update() {
    this.y -= this.speed

    const fadeZone = height * 0.2
    this.currentOpacity = this.opacity
    if (this.y < fadeZone) {
      this.currentOpacity = this.opacity * (this.y / fadeZone)
    }

    if (this.y < -10) {
      this.init(false)
    }
  }

  draw() {
    ctx.fillStyle = `rgba(255, 255, 255, ${this.currentOpacity})`
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
  }
}

function resize() {
  width = window.innerWidth
  height = window.innerHeight
  starCanvasRef.value!.width = width
  starCanvasRef.value!.height = height
}

function initStars() {
  stars = []
  for (let i = 0; i < props.starCount; i++) stars.push(new Star())
}

function animate() {
  ctx.clearRect(0, 0, width, height)

  stars.forEach((star) => {
    star.update()
    star.draw()
  })

  rafId = requestAnimationFrame(animate)
}

const onResize = () => resize()

onMounted(() => {
  ctx = starCanvasRef.value!.getContext('2d')!
  resize()
  initStars()
  window.addEventListener('resize', onResize)
  animate()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <div class="w-full h-full pointer-events-none absolute inset-0 z-0 overflow-hidden">
    <canvas
      ref="starCanvasRef"
      class="star-canvas block absolute top-0 left-0"
    />
  </div>
</template>
