import { isArray, isEmpty } from 'lodash-es'

import { DesktopRecordActionType, RecordActionType, WebRecordActionType } from '@/constants/record'
import { useElementsStore } from '@/stores/useElementsStore'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import type { PickElementType } from '@/types/resource'
import { addAtomData, group } from '@/views/Arrange/components/flow/hooks/useFlow'

/**
 * Add recorded actions to the workflow and group the generated atoms.
 */
export async function addRecordAtomData(data: { action: RecordActionType, pickInfo: string }[]) {
  if (isEmpty(data))
    return

  const { addNewElement, requestAllElements } = useElementsStore()

  useProcessStore().saveProject()

  /**
   * Build atoms in reverse order so insertion after the current selection keeps
   * the visible recording order.
   */
  const newData = data.map((item) => {
    const pickInfo = JSON.parse(item.pickInfo) as PickElementType
    const actions = pickInfo.type === 'web' ? WebRecordActionType : DesktopRecordActionType
    const addAtomType = actions[item.action]

    return { ...item, pickInfo, addAtomType }
  }).filter(item => !!item.addAtomType).reverse()

  const addAtom = async () => {
    const list = await Promise.all(newData.map(item => addAtomData(item.addAtomType)))
    group(list.flat(1).map(atom => atom.id))
    return list
  }

  const [pickElements, atoms] = await Promise.all([
    Promise.all(newData.map(item => addNewElement(item.pickInfo))),
    addAtom(),
  ])

  // Refresh the element manager list.
  requestAllElements()

  pickElements.forEach((item, index) => {
    const atom = atoms[index]
    const action = newData[index].action
    const elementValue: RPA.AtomFormItemResult[] = [{ type: 'element', value: item.name, data: item.elementId }]
    isArray(atom) && atom.forEach(it => modifyFormConfig(it, elementValue, action))
  })
}

// Populate the generated atom form fields for the recorded action.
function modifyFormConfig(atom: RPA.Atom, elementValue: RPA.AtomFormItemResult[], action: RecordActionType) {
  const { setFormItemValue } = useFlowStore()

  // 가져오기문서문자내용 / 웹 페이지 - 요소
  if (action === RecordActionType.GET_ELEMENT_TEXT && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('get_type', 'getText', atom.id)
    return
  }
  // 가져오기 코드 / 웹 페이지 - 요소
  if (action === RecordActionType.GET_ELEMENT_CODE && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('get_type', 'getHtml', atom.id)
    return
  }
  // 가져오기 연결주소 / 웹 페이지 - 요소
  if (action === RecordActionType.GET_ELEMENT_LINK && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('get_type', 'getLink', atom.id)
    return
  }
  // 가져오기요소속성 / 웹 페이지 - 요소
  if (action === RecordActionType.GET_ELEMENT_ATTR && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('get_type', 'getAttribute', atom.id)
    return
  }
  // 입력 / 웹 페이지 - 입력란
  if (action === RecordActionType.INPUT && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    return
  }
  // 마우스동작까지안 / 웹 페이지 - 마우스중지에서요소위
  if (action === RecordActionType.MOUSE_MOVE && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    return
  }
  // 클릭(왼쪽 버튼) / 웹 페이지 - 클릭요소
  if (action === RecordActionType.CLICK_LEFT && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('button_type', 'click', atom.id)
    return
  }
  // 더블클릭 / 웹 페이지 - 클릭요소
  if (action === RecordActionType.CLICK_LEFT_RIGHT && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('button_type', 'dbclick', atom.id)
    return
  }
  // 오른쪽 버튼클릭 / 웹 페이지 - 클릭요소
  if (action === RecordActionType.CLICK_RIGHT && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('button_type', 'right', atom.id)
    return
  }
  // 대기요소출력 / 웹 페이지 - 대기요소
  if (action === RecordActionType.WAIT_ELEMENT_SHOW && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('ele_status', 'y', atom.id)
    return
  }
  // 대기요소메시지실패 / 웹 페이지 - 대기요소
  if (action === RecordActionType.WAIT_ELEMENT_HIDE && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
    setFormItemValue('ele_status', 'n', atom.id)
    return
  }
  // 스크린샷 / 웹 페이지 - 선택요소스크린샷
  if (action === RecordActionType.SNAPSHOT && atom.key === WebRecordActionType[action]) {
    setFormItemValue('element_data', elementValue, atom.id)
  }

  // *************************************************  ***************************************************

  // 가져오기문서문자내용 /  - 가져오기요소텍스트
  if (action === RecordActionType.GET_ELEMENT_TEXT && atom.key === DesktopRecordActionType[action]) {
    setFormItemValue('pick', elementValue, atom.id)
    return
  }
  // 입력 /  - 입력란
  if (action === RecordActionType.INPUT && atom.key === DesktopRecordActionType[action]) {
    setFormItemValue('pick', elementValue, atom.id)
    return
  }
  // 마우스동작까지안 /  - 마우스중지요소
  if (action === RecordActionType.MOUSE_MOVE && atom.key === DesktopRecordActionType[action]) {
    setFormItemValue('pick', elementValue, atom.id)
    return
  }
  // 클릭(왼쪽 버튼) /  - 클릭요소
  if (action === RecordActionType.CLICK_LEFT && atom.key === DesktopRecordActionType[action]) {
    setFormItemValue('pick', elementValue, atom.id)
    setFormItemValue('click_button', 'left', atom.id)
    setFormItemValue('click_type', 'click', atom.id)
    return
  }
  // 더블클릭 /  - 클릭요소
  if (action === RecordActionType.CLICK_LEFT_RIGHT && atom.key === DesktopRecordActionType[action]) {
    setFormItemValue('pick', elementValue, atom.id)
    setFormItemValue('click_button', 'left', atom.id)
    setFormItemValue('click_type', 'double_click', atom.id)
    return
  }
  // 오른쪽 버튼클릭 /  - 클릭요소
  if (action === RecordActionType.CLICK_RIGHT && atom.key === DesktopRecordActionType[action]) {
    setFormItemValue('pick', elementValue, atom.id)
    setFormItemValue('click_button', 'right', atom.id)
    setFormItemValue('click_type', 'click', atom.id)
    return
  }
  // 스크린샷 /  - 요소스크린샷
  if (action === RecordActionType.SNAPSHOT && atom.key === DesktopRecordActionType[action]) {
    setFormItemValue('pick', elementValue, atom.id)
  }
}
