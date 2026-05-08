import hotkeys from 'hotkeys-js'
import { cloneDeep } from 'lodash-es'
import { ref } from 'vue'

import { getUserSetting } from '@/api/setting'
import { SCOPE, shortcuts } from '@/constants/shortcuts'

type HotkeyCallback = () => void

const hotkeySetting = ref(cloneDeep(shortcuts))
const hotkeyFnMap = ref<Record<string, HotkeyCallback>>({})

function getHotkeySetting(autoRegister = false) {
  getUserSetting().then((res: any) => {
    if (res) {
      const localShortcuts = res.shortcutConfig || {}
      Object.keys(hotkeySetting.value).forEach((key) => {
        const currentItem = hotkeySetting.value[key]
        const localItem = localShortcuts[key]
        if (localItem) {
          if (autoRegister && hotkeyFnMap.value[currentItem.id] && localItem.value !== currentItem.value) {
            const oldHotkey = currentItem.text.replace(/\s/g, '')
            hotkeys.unbind(oldHotkey)
            const hotkey = localItem.text.replace(/\s/g, '')
            hotkeys(hotkey, SCOPE, hotkeyFnMap.value[currentItem.id])
          }
          currentItem.value = localItem.value
          currentItem.text = localItem.text
        }
      })
    }
  })
}

getHotkeySetting()

function updateHotkeysSetting() {
  hotkeys.setScope(SCOPE)
  getHotkeySetting(true)
}

hotkeys.setScope(SCOPE)

function registerHotkey(id: string, callback: () => void) {
  const setting = hotkeySetting.value[id]
  if (!setting?.value) {
    return console.error(`등록된 단축키 설정을 찾을 수 없습니다: ${id}. shortcuts 설정을 확인하세요.`)
  }
  hotkeyFnMap.value[id] = callback
  const hotkey = setting.text.replace(/\s/g, '')
  hotkeys(hotkey, SCOPE, () => {
    hotkeyFnMap.value[id]?.()
  })
}

function unregisterHotkey(id: string) {
  const setting = hotkeySetting.value[id]
  if (!setting?.value) {
    return console.error(`등록된 단축키 설정을 찾을 수 없습니다: ${id}. shortcuts 설정을 확인하세요.`)
  }
  delete hotkeyFnMap.value[id]
  const hotkey = setting.text.replace(/\s/g, '')
  hotkeys.unbind(hotkey, SCOPE)
}

export {
  registerHotkey,
  unregisterHotkey,
  updateHotkeysSetting,
}
