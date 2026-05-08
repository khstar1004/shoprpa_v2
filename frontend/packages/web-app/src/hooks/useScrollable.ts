import { useResizeObserver } from '@vueuse/core'
import type { Ref } from 'vue'
import { ref } from 'vue'

export function useScrollable(el: Ref<HTMLElement | null>) {
  const isScrollable = ref(false)

  const check = () => {
    if (!el.value)
      return

    // 감지수직직선
    const canScrollY = el.value.scrollHeight > el.value.clientHeight
    // 감지수평평면
    const canScrollX = el.value.scrollWidth > el.value.clientWidth

    // 가져오기 사용의 CSS 속성
    const style = window.getComputedStyle(el.value)
    const overflowY = style.overflowY
    const overflowX = style.overflowX

    isScrollable.value
      = (canScrollY && (overflowY === 'auto' || overflowY === 'scroll'))
        || (canScrollX && (overflowX === 'auto' || overflowX === 'scroll'))
  }

  // 요소변수
  useResizeObserver(el, check)

  // 실행일감지
  check()

  return isScrollable
}
