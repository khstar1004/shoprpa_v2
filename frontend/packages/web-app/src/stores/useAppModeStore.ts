import { defineStore } from 'pinia'
import { ref } from 'vue'

// app 방식정보
export const useAppModeStore = defineStore('appMode', () => {
  const appMode = ref('normal') // normal-통신방식 | scheduling-스케줄링방식
  const setAppMode = (mode: string) => {
    appMode.value = mode
  }

  return {
    appMode,
    setAppMode,
  }
})