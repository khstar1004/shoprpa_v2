import type { RouteLocationAsRelativeGeneric } from 'vue-router'

import { refreshModal } from '@/utils/antd.common'

import router from '@/router'

// 변환까지경로
export async function useRoutePush(to: RouteLocationAsRelativeGeneric) {
  const currentName = router.currentRoute.value.name
  // debugger
  if (currentName === to.name)
    return // 중지재복사변환

  try {
    await router.push(to)
  }
  catch (err) {
    if (err.toString().includes('Failed to fetch dynamically')) {
      refreshModal()
    }
  }
}

// 가져오기경로테이블
export function useRouteList() {
  return router.getRoutes()
}

// 돌아가기
export function useRouteBack() {
  router.back()
}
