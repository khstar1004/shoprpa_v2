import type { BasicColorSchema } from '@vueuse/core'
import { message } from 'ant-design-vue'
import deepmerge from 'deepmerge'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { autoStartDisable, autoStartEnable, autoStartStatus, getUserSetting, setUserSetting } from '@/api/setting'

export type Theme = BasicColorSchema

export const DEFAULT_FORM: RPA.VideoFormMap = {
  saveType: true,
  enable: false,
  scene: 'fail',
  cutTime: 30,
  fileClearTime: 7,
  // maxRecordingTime: 10,
  filePath: '',
}

const useUserSettingStore = defineStore('useUserSetting', () => {
  const userSetting = ref<RPA.UserSetting>({
    commonSetting: { // 일반
      startupSettings: false, // 시작 - 연결가져오기저장, true 열기시작시작, false-닫기열기기기시작
      closeMainPage: false, // true-소  false-출력사용
      hideLogWindow: false, // 런타임의오른쪽아래역할로그창 true-열기시작  false-닫기
      hideDetailLogWindow: false, // 실행의로그창 true-열기시작  false-닫기
      autoSave: true, // 저장 true-열기시작  false-닫기
      theme: 'auto',
    },
    shortcutConfig: {}, // 빠름
    videoForm: DEFAULT_FORM, // 기록
    msgNotifyForm: {}, // 메시지알림
  })

  // 가져오기일반
  const getSetting = async () => {
    const [autostart, setting] = await Promise.all([
      autoStartStatus(),
      getUserSetting(),
    ])

    userSetting.value = deepmerge.all<RPA.UserSetting>([
      userSetting.value,
      setting,
      { commonSetting: { startupSettings: autostart } },
    ],
    )
  }

  const saveUserSetting = (params: object) => {
    userSetting.value = {
      ...userSetting.value,
      ...params,
    }
    setUserSetting(userSetting.value)
  }

  // 수정일반
  const changeCommonConfig = async (key: string, value: boolean) => {
    if (key === 'startupSettings') {
      const func = value ? autoStartEnable : autoStartDisable
      const res = await func()
      message.success(res.data.tips)
      getSetting()

      return
    }

    saveUserSetting({
      commonSetting: {
        ...userSetting.value.commonSetting,
        [key]: value,
      },
    })
  }

  // 실행 결과후여부열기로그팝업
  const openLogModalAfterRun = computed(() => !userSetting.value.commonSetting.hideDetailLogWindow)

  getSetting()

  return {
    userSetting,
    openLogModalAfterRun,
    saveUserSetting,
    changeCommonConfig,
  }
})

export default useUserSettingStore
