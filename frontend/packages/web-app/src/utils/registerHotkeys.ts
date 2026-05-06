import hotkeys from 'hotkeys-js'
import { cloneDeep } from 'lodash-es'
import { ref } from 'vue'

import { getUserSetting } from '@/api/setting'
import { SCOPE, shortcuts } from '@/constants/shortcuts'

const hotkeySetting = ref(cloneDeep(shortcuts))
const hotkeyFnMap = ref({}) // 완료회원가입빠름의돌아가기조정데이터

// // 비고판매모든빠름
// const unregisterAll = () => {
//   Object.keys(hotkeyFnMap.value).forEach(id=>{
//     const hotkey = hotkeySetting.value[id].text.replace(/\s/g, "")
//     hotkeys.unbind(hotkey)
//   })
// }

// // 회원가입빠름
// const registerAll = () => {
//   Object.keys(hotkeyFnMap.value).forEach(id=>{
//     const hotkey = hotkeySetting.value[id].text.replace(/\s/g, "")
//     hotkeys(hotkey, hotkeyFnMap[id])
//   })
// }

function getHotkeySetting(autoRegister = false) {
  getUserSetting().then((res: any) => {
    if (res) {
      const localShortcuts = res.shortcutConfig || {}
      Object.keys(hotkeySetting.value).forEach((key) => {
        const currentItem = hotkeySetting.value[key]
        const localItem = localShortcuts[key]
        if (localItem) {
          // 비고판매빠름
          if (autoRegister && hotkeyFnMap[currentItem.id] && localItem.value !== currentItem.value) {
            const oldHotkey = currentItem.text.replace(/\s/g, '')
            hotkeys.unbind(oldHotkey)
            const hotkey = localItem.text.replace(/\s/g, '')
            hotkeys(hotkey, SCOPE, hotkeyFnMap[currentItem.id])
          }
          // 저장에서본데이터가능덮어쓰기데이터
          currentItem.value = localItem.value
          currentItem.text = localItem.text
        }
      })
    }
  })
}

// 빠름
getHotkeySetting()

// 업데이트빠름
function updateHotkeysSetting() {
  hotkeys.setScope(SCOPE)
  getHotkeySetting(true)
}

hotkeys.setScope(SCOPE)

// 회원가입단일개빠름
function registerHotkey(id: string, callback: () => void) {
  if (!hotkeySetting.value[id].value) {
    return console.error(`찾을 수 없는 빠름매칭: ${id}, 요청에서config폴더아래추가빠름매칭`)
  }
  hotkeyFnMap[id] = callback
  const hotkey = hotkeySetting.value[id].text.replace(/\s/g, '')
  hotkeys(hotkey, SCOPE, () => {
    hotkeyFnMap[id]()
  })
}

// 비고판매단일개빠름
function unregisterHotkey(id: string) {
  if (!hotkeySetting.value[id].value) {
    return console.error(`찾을 수 없는 빠름매칭: ${id}, 요청에서config폴더아래추가빠름매칭`)
  }
  delete hotkeyFnMap[id]
  const hotkey = hotkeySetting.value[id].text.replace(/\s/g, '')
  hotkeys.unbind(hotkey, SCOPE)
}

export {
  registerHotkey,
  unregisterHotkey,
  updateHotkeysSetting,
}