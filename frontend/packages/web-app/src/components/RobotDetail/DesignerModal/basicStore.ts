import { createInjectionState } from '@vueuse/core'
import { onBeforeMount, ref, shallowRef } from 'vue'

import { getMarketRobotDetail, getMyRobotDetail } from '@/api/robot'
import { VIEW_OWN } from '@/constants/resource'

import type { Version } from '../components/VersionTable/index.vue'

interface BasicContentData {
  createTime: string // 생성 시간
  creatorName: string // 생성자이름
  introduction: string // 
  name: string // 사용이름
  version: number // 버전
  filePath: string // 파일 경로
  fileName: string // 파일이름
  videoName?: string // 이름
  videoPath?: string // 경로
  useDescription: string // 사용설명
  sourceName?: string // 
  versionList?: Version[] // 버전목록
}

const [useProvideBasicStore, useBasicStore] = createInjectionState((robotId: string, source: string) => {
  const loading = ref(false)
  const data = shallowRef<BasicContentData>(null)

  onBeforeMount(async () => {
    loading.value = true
    const apiFn = source === VIEW_OWN ? getMyRobotDetail : getMarketRobotDetail
    data.value = await apiFn(robotId)
    loading.value = false
  })

  return { loading, data }
})

export { useBasicStore, useProvideBasicStore }