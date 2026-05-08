export enum RecordActionType {
  GET_ELEMENT_INFO = 'get-element-info',
  GET_ELEMENT_TEXT = 'get-element-text',
  GET_ELEMENT_CODE = 'get-element-code',
  GET_ELEMENT_LINK = 'get-element-link',
  GET_ELEMENT_ATTR = 'get-element-attr',
  INPUT = 'input',
  MOUSE_MOVE = 'mouse-move',
  CLICK = 'click',
  CLICK_LEFT = 'click-left',
  CLICK_LEFT_RIGHT = 'click-left-right',
  CLICK_RIGHT = 'click-right',
  WAIT_ELEMENT = 'wait-element',
  WAIT_ELEMENT_SHOW = 'wait-element-show',
  WAIT_ELEMENT_HIDE = 'wait-element-hide',
  SNAPSHOT = 'snapshot',
}

// 웹 페이지 조작
export const WebRecordActionType: Partial<Record<RecordActionType, string>> = {
  [RecordActionType.GET_ELEMENT_TEXT]: 'BrowserElement.element_operation',
  [RecordActionType.GET_ELEMENT_CODE]: 'BrowserElement.element_operation',
  [RecordActionType.GET_ELEMENT_LINK]: 'BrowserElement.element_operation',
  [RecordActionType.GET_ELEMENT_ATTR]: 'BrowserElement.element_operation',
  [RecordActionType.INPUT]: 'BrowserElement.input',
  [RecordActionType.MOUSE_MOVE]: 'BrowserElement.hover_over',
  [RecordActionType.CLICK_LEFT]: 'BrowserElement.click',
  [RecordActionType.CLICK_LEFT_RIGHT]: 'BrowserElement.click',
  [RecordActionType.CLICK_RIGHT]: 'BrowserElement.click',
  [RecordActionType.WAIT_ELEMENT_SHOW]: 'BrowserElement.wait_element',
  [RecordActionType.WAIT_ELEMENT_HIDE]: 'BrowserElement.wait_element',
  [RecordActionType.SNAPSHOT]: 'BrowserElement.screenshot',
}

//
export const DesktopRecordActionType: Partial<Record<RecordActionType, string>> = {
  [RecordActionType.GET_ELEMENT_TEXT]: 'WinEle.get_element_text',
  [RecordActionType.INPUT]: 'WinEle.input_text_element',
  [RecordActionType.MOUSE_MOVE]: 'WinEle.hover_element',
  [RecordActionType.CLICK_LEFT]: 'WinEle.click_element',
  [RecordActionType.CLICK_LEFT_RIGHT]: 'WinEle.click_element',
  [RecordActionType.CLICK_RIGHT]: 'WinEle.click_element',
  [RecordActionType.SNAPSHOT]: 'WinEle.screenshot_element',
}

export const RecordActionMap: Record<RecordActionType, { icon: string, label: string }> = {
  [RecordActionType.GET_ELEMENT_INFO]: {
    icon: 'get-element-text-web',
    label: '요소 정보 가져오기',
  },
  [RecordActionType.GET_ELEMENT_TEXT]: {
    icon: 'get-element-text-web',
    label: '텍스트 가져오기',
  },
  [RecordActionType.GET_ELEMENT_CODE]: {
    icon: 'get-element-text-web',
    label: '코드 가져오기',
  },
  [RecordActionType.GET_ELEMENT_LINK]: {
    icon: 'get-element-text-web',
    label: '링크 주소 가져오기',
  },
  [RecordActionType.GET_ELEMENT_ATTR]: {
    icon: 'get-element-text-web',
    label: '요소 속성 가져오기',
  },
  [RecordActionType.INPUT]: {
    icon: 'fill-input-web',
    label: '입력',
  },
  [RecordActionType.MOUSE_MOVE]: {
    icon: 'mouse-move',
    label: '마우스 이동',
  },
  [RecordActionType.CLICK]: {
    icon: 'click-element-web',
    label: '클릭',
  },
  [RecordActionType.CLICK_LEFT]: {
    icon: 'click-element-web',
    label: '클릭',
  },
  [RecordActionType.CLICK_LEFT_RIGHT]: {
    icon: 'click-element-web',
    label: '더블클릭',
  },
  [RecordActionType.CLICK_RIGHT]: {
    icon: 'click-element-web',
    label: '오른쪽 버튼클릭',
  },
  [RecordActionType.WAIT_ELEMENT]: {
    icon: 'wait-element-web',
    label: '요소 대기',
  },
  [RecordActionType.WAIT_ELEMENT_SHOW]: {
    icon: 'wait-element-web',
    label: '요소 표시 대기',
  },
  [RecordActionType.WAIT_ELEMENT_HIDE]: {
    icon: 'wait-element-web',
    label: '요소 숨김 대기',
  },
  [RecordActionType.SNAPSHOT]: {
    icon: 'pick-element-screenshot-web',
    label: '스크린샷',
  },
}

// 창통신파일
export enum RECORD_EVENT {
  SHOW_MENU = 'show-menu', // 메뉴
  HIDE_MENU = 'hide-menu', // 메뉴
  PAUSE_PICK = 'pause-pick', // 일시중지선택
  RESUME_PICK = 'resume-pick', // 복사선택
  CLICK_ACTION = 'click-action', // 클릭메뉴
}
