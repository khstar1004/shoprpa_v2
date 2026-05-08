import { onBeforeUnmount, ref } from 'vue'

import { updateHotkeysSetting } from '@/utils/registerHotkeys'

import type { ShortcutItemMap } from '@/components/ShortcutInput/types.ts'
import { commonKeys, shortcuts } from '@/constants/shortcuts'
import useUserSettingStore from '@/stores/useUserSetting.ts'

export function useShortcutData() {
  const shortcutForm = ref()
  const formData = ref(shortcuts)

  // 가져오기본빠름, 업데이트현재수정의빠름
  const saveShortCutData = () => {
    const shortcutFormData = {}
    Object.keys(formData.value).forEach((shortKey) => {
      shortcutFormData[shortKey] = {
        value: formData.value[shortKey].value,
        text: formData.value[shortKey].text,
      }
    })
    const newSetting = { shortcutConfig: shortcutFormData }
    useUserSettingStore().saveUserSetting(newSetting)
    updateHotkeysSetting()
  }

  // 본및현재테이블단일의빠름데이터, 사용직선연결값완료데이터불가업데이트속성key
  const getShortCutData = () => {
    const localShortcuts = useUserSettingStore().userSetting.shortcutConfig || {}
    Object.keys(formData.value).forEach((shortKey) => {
      if (localShortcuts[shortKey]) {
        // 저장에서본데이터가능덮어쓰기데이터
        formData.value[shortKey].value = localShortcuts[shortKey].value
        formData.value[shortKey].text = localShortcuts[shortKey].text
      }
    })
    validateAll()
  }

  const validate = (itemData: ShortcutItemMap) => {
    // validateText저장인증결과
    let validateText = ''
    // 행인증
    const value = itemData.text.replace(/\s/g, '').toLowerCase()
    if (!['클릭', '입력', '입력새의'].includes(value)) {
      const inx = commonKeys.indexOf(value)
      if (inx > -1) {
        validateText += `빠름및일반빠름또는소프트파일내부빠름${commonKeys[inx]}!`
      }
      Object.keys(formData.value).forEach((key) => {
        const item = formData.value[key]
        if (item.id !== itemData.id) {
          const { text, name } = item
          if (value === text.replace(/\s/g, '').toLowerCase()) {
            validateText += `빠름및${name}!`
          }
        }
      })
    }

    formData.value[itemData.id].validate = validateText
  }

  const validateAll = () => {
    Object.keys(formData.value).forEach((key) => {
      validate(formData.value[key])
    })
  }

  const setActive = (itemData: ShortcutItemMap, status = true) => {
    Object.keys(formData.value).forEach((key) => {
      formData.value[key].active = itemData.id === formData.value[key].id ? status : false
    })
  }

  getShortCutData()

  onBeforeUnmount(() => {
    saveShortCutData()
  })

  return {
    shortcutForm,
    formData,
    validate,
    setActive,
    validateAll,
  }
}
