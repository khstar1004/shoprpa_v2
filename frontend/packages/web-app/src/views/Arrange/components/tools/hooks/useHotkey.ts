import { onBeforeUnmount } from 'vue'

import { registerHotkey, unregisterHotkey } from '@/utils/registerHotkeys'

import type { Fun } from '@/types/common'
import type { ArrangeTools } from '@/views/Arrange/types/arrangeTools'

export function useHotkey(toolsLeft: ArrangeTools[], hotkeyCallback: Fun) {
 const resister = (hotkeyList: ArrangeTools[]) => {
 hotkeyList.forEach((item) => {
 item.hotkey && registerHotkey(item.hotkey, () => {
 // 회원가입시의hotkey의데이터및시변수의toolsLeft의데이터가능아니오일, 으로필요다시 가져오기
 hotkeyCallback(toolsLeft.find(i => i.key === item.key))
 })
 })
 }

 onBeforeUnmount(() => {
 toolsLeft.forEach((item) => {
 item.hotkey && unregisterHotkey(item.hotkey)
 })
 })

 resister(toolsLeft)
}
