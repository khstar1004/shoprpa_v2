import hotkeys from 'hotkeys-js'

import { SCOPE } from '@/constants/shortcuts'

let itemScope = ''

function resetKeyboard() {
  if (itemScope) {
    hotkeys.unbind('*', itemScope)
    hotkeys.deleteScope(itemScope)
    hotkeys.setScope(SCOPE)
  }
}

// function isBodyOrAntModal(e: Event): boolean {
//   // 사용유형확인 e.target 예 HTMLElement 유형
//   if ((e.target as HTMLElement).tagName === 'BODY') {
//     return true
//   }

//   // 조회 e.target 여부로 SVG 요소있음 className.baseVal, 예결과예이면반환 false
//   // 아니오이면조회 e.target 여부패키지 "ant-modal" 유형이름
//   const target = e.target as HTMLElement | SVGElement
//   if (target instanceof SVGElement && target.className instanceof SVGAnimatedString) {
//     return false
//   }

//   return target.className?.indexOf('ant-modal') > -1
// }

function getKeyboard(data, callback) {
  // 의data여부있음id속성, 있음의추가위
  if (!(Object.prototype.hasOwnProperty.call(data, 'id') && data.id)) {
    data.id = Date.now().toString(36)
  }
  // 통신경과setScope지정scope
  hotkeys.setScope(data.id)
  itemScope = data.id
  let keyboard = ''
  let keyCodeValue: string | number = ''
  let keyValue = ''

  hotkeys('*', data.id, (e: KeyboardEvent) => {
    // if (isBodyOrAntModal(e)) {
    if (![16, 17, 18, 91].includes(e.keyCode)) {
      const k = e.key === ' ' ? 'space' : e.key
      keyboard = k
      keyCodeValue = e.keyCode
      keyValue = k
      if (e.ctrlKey) {
        // keyCode 17
        keyValue = `Ctrl,${keyboard}`
        keyboard = `Ctrl + ${keyboard}`
        keyCodeValue = `17,${keyCodeValue}`
      }
      if (e.shiftKey) {
        // keyCode 16
        keyValue = `Shift,${keyboard}`
        keyboard = `Shift + ${keyboard}`
        keyCodeValue = `16,${keyCodeValue}`
      }
      if (e.altKey) {
        // keyCode 18
        keyValue = `Alt,${keyboard}`
        keyboard = `Alt + ${keyboard}`
        keyCodeValue = `18,${keyCodeValue}`
      }
      callback({
        id: data.id,
        text: keyboard,
        value: keyValue.replaceAll(' + ', ','),
      })
    }
    // }
  })
}

export {
  getKeyboard,
  resetKeyboard,
}
