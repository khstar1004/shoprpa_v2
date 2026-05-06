import { NiceModal } from '@rpa/components'
import { message } from 'ant-design-vue'

import i18next from '@/plugins/i18next'

import BUS from '@/utils/eventBus'

import { validateContractResult } from '@/api/contract'
import { getHTMLClip } from '@/api/resource'
import { ATOM_FORM_TYPE, ELEMENT_IN_TYPE, GLOBAL_VAR_IN_TYPE, OTHER_IN_TYPE, PARAMETER_VAR_IN_TYPE, PY_IN_TYPE, VAR_IN_TYPE } from '@/constants/atom'
import { useFlowStore } from '@/stores/useFlowStore'
import { CustomDialog } from '@/views/Arrange/components/customDialog'
import { UserFormDialogModal } from '@/views/Arrange/components/customDialog/components'
import { INPUT_NUMBER_TYPE_ARR, ORIGIN_SPECIAL, ORIGIN_VAR, PROCESS_VAR_TYPE } from '@/views/Arrange/config/atom'
import { backContainNodeIdx } from '@/views/Arrange/utils/flowUtils'

import { ContractValidateModal, EmailTextReplaceModal, TextareaModal } from '../modals'

import { createDom, setEditTextContent } from './useAtomVarPopover'
import useFormPick from './useFormPick'
import { getRealValue, getUserFormOption } from './usePreview'

const hasDataCategoryType = [VAR_IN_TYPE, GLOBAL_VAR_IN_TYPE, PARAMETER_VAR_IN_TYPE, ELEMENT_IN_TYPE]

export function generateInputVal(itemData: RPA.AtomDisplayItem) {
 let result = ''
 const { formType: { type, params }, value } = itemData
 if (!Array.isArray(value))
 return value
 if (Object.is(type, ATOM_FORM_TYPE.RANGEDATEPICKER)) {
 return [itemData.value[0], itemData.value[1]]
 }
 if ((Object.is(type, ATOM_FORM_TYPE.SELECT) && params?.multiple) || (Object.is(type, ATOM_FORM_TYPE.CHECKBOXGROUP))) {
 return itemData.value
 }
 value.forEach((item) => {
 // 예변수유형아니오예출력변수추가방식
 if (hasDataCategoryType.includes(item.type) && !Object.is(type, ATOM_FORM_TYPE.RESULT)) {
 const { data, type, value } = item
 const dataId = data ? `data-id="${data}"` : ''
 result += `<hr class="ui-at" ${escapeHTML(dataId)} data-category="${escapeHTML(type)}" data-name="${escapeHTML(value)}"></hr>`
 }
 else {
 result += item.value
 }
 })

 return result
}

function escapeHTML(str: string) {
 const entityMap = {
 '&': '&amp;',
 '<': '&lt;',
 '>': '&gt;',
 '"': '&quot;',
 '\'': '&#x27;',
 '/': '&#x2F;',
 }
 return String(str).replace(/[&<>"'/]/g, s => entityMap[s])
}

export function handlePaste(event: ClipboardEvent, itemData: RPA.AtomDisplayItem) {
 event.preventDefault()
 const clipboardData = event.clipboardData
 if (!clipboardData)
 return
 const textData = clipboardData.getData('text/plain')
 if (textData) {
 insertHtmlAtCaret(textData)
 let target = event.currentTarget as HTMLDivElement
 if (target.nodeName === 'BR') {
 target = target.parentNode as HTMLDivElement
 }
 generateHtmlVal(target as HTMLDivElement, itemData)
 }
}

function insertHtmlAtCaret(html: string) {
 const selection = window.getSelection()
 if (!selection || selection.rangeCount === 0)
 return
 const range = selection.getRangeAt(0)
 range.deleteContents()
 const tempDiv = document.createElement('div')
 tempDiv.innerHTML = html

 const startContainer = range.startContainer
 const startOffset = range.startOffset
 const textContent = tempDiv.textContent || tempDiv.innerText || ''
 const isPlainText = tempDiv.children.length === 0 && textContent === html.trim()

 // 만약전예텍스트점, 삽입의예텍스트, 이면병합
 if (startContainer.nodeType === Node.TEXT_NODE && isPlainText) {
 const textNode = startContainer as Text
 const beforeText = textNode.textContent?.substring(0, startOffset) || ''
 const afterText = textNode.textContent?.substring(startOffset) || ''

 textNode.textContent = beforeText + textContent + afterText

 const newOffset = beforeText.length + textContent.length
 range.setStart(textNode, newOffset)
 range.collapse(true)
 selection.removeAllRanges()
 selection.addRange(range)
 return
 }

 // 아니오이면삽입새점
 const fragment = document.createDocumentFragment()
 let node = tempDiv.firstChild
 while (node) {
 fragment.appendChild(node)
 node = tempDiv.firstChild
 }
 range.insertNode(fragment)
 range.collapse(false)
 selection.removeAllRanges()
 selection.addRange(range)
}

export function generateHtmlVal(target: HTMLDivElement, itemData: RPA.AtomDisplayItem, atomId?: string) {
 const { isExpr, formType, types } = itemData
 const nodeList = target.childNodes

 if (itemData.customizeTip)
 delete itemData.customizeTip
 const result: RPA.AtomFormItemResult[] = []
 // 비고: 아니오버전의 Chrome 으로및아니오브라우저에서관리 contenteditable 빈상태시행로가능아니오
 // 1. 없음점(브라우저/버전)
 // 2. 있음일개 BR 태그(Chrome 대기브라우저자체동작삽입의위치기호)
 const hasOnlyBr = nodeList.length === 1 && nodeList[0].nodeName === 'BR'
 if (nodeList.length === 0 || hasOnlyBr) {
 result.push({ type: OTHER_IN_TYPE, value: '' })
 }
 else {
 nodeList.forEach((node) => {
 const obj: RPA.AtomFormItemResult = { type: OTHER_IN_TYPE, value: '' } // 기본값예other유형
 if (node.nodeType === Node.TEXT_NODE) { // 테이블표시예텍스트점
 const textNode = node as Text
 obj.value = textNode.nodeValue
 // 해당값필요전송숫자유형방식아니오예변수아니오예py
 if (INPUT_NUMBER_TYPE_ARR.includes(types) && !Object.is(formType.type, ATOM_FORM_TYPE.RESULT) && Object.is(obj.type, OTHER_IN_TYPE) && !isExpr) {
 const numberPattern = /^-?\d*\.?\d*$/
 if (!numberPattern.test(obj.value))
 itemData.customizeTip = i18next.t('atomForm.onlyNumberTip')
 }
 if (obj.value)
 result.push(obj)
 }
 else if (node.nodeType === Node.ELEMENT_NODE) { // 테이블표시예요소점
 const elementNode = node as HTMLElement
 if (elementNode.classList.contains('ui-at')) {
 const id = elementNode.dataset.id
 const category = elementNode.dataset.category
 obj.type = category || VAR_IN_TYPE
 obj.value = elementNode.dataset.name
 if (id && id !== 'undefined')
 obj.data = id
 result.push(obj)
 }
 else {
 elementNode.nodeName !== 'BR' && result.push(obj)
 }
 }
 })
 }

 if (Object.is(formType.type, ATOM_FORM_TYPE.RESULT)) {
 // 예출력해당값예변수유형
 result[0].type = VAR_IN_TYPE
 }

 itemData.value = result

 if (isExpr)
 setPyModeVal(itemData) // 해당값예python유형

 syncCurrentAtomData(itemData, false, atomId)
}

export function setPyModeVal(itemData: RPA.AtomDisplayItem) {
 const { value, isExpr } = itemData
 const obj = { type: isExpr ? PY_IN_TYPE : OTHER_IN_TYPE, value: '' }
 // py방식있음일개값
 if (Array.isArray(value)) {
 value.forEach((item) => {
 obj.value += item.value
 })
 }
 itemData.value = [obj]
}

// 근거유형출력테이블단일아니오결과
export function formBtnHandle(itemData: RPA.AtomDisplayItem, itemType: string, extraProp: any) {
 const { pickLoading, elementPickModal, id } = extraProp
 const flowStore = useFlowStore()

 switch (itemType) {
 case ATOM_FORM_TYPE.PYTHON:
 itemData.isExpr = !itemData.isExpr
 setPyModeVal(itemData)
 setEditTextContent(itemData.key, itemData.value[0].value)
 syncCurrentAtomData(itemData, false)
 break
 case ATOM_FORM_TYPE.PICK:
 BUS.$once('batch-done', (res: any) => {
 if (itemData.key === 'batch_data') {
 itemData.value = [{ type: ELEMENT_IN_TYPE, value: res.value, data: res.data }]
 flowStore.setFormItemValue(itemData.key, itemData.value, id || flowStore.activeAtom.id)
 }
 })
 BUS.$once('pick-done', (res: any) => {
 if (itemData.key === 'batch_data')
 return // 가져오기아니오에서안관리
 itemData.value = [{ type: ELEMENT_IN_TYPE, value: res.value, data: res.data }]
 flowStore.setFormItemValue(itemData.key, itemData.value, id || flowStore.activeAtom.id)
 })
 useFormPick(itemData.formType.params.use, pickLoading, elementPickModal, itemData)
 break
 case ATOM_FORM_TYPE.CVPICK:
 BUS.$once('cv-pick-done', (res: any) => {
 itemData.value = [{ type: ELEMENT_IN_TYPE, value: res.value, data: res.data }]
 flowStore.setFormItemValue(itemData.key, itemData.value, id || flowStore.activeAtom.id)
 })
 break
 case ATOM_FORM_TYPE.MOUSEPOSITION:
 BUS.$once('pick-done', (res: any) => {
 const { data: { x, y } } = res
 console.log('MOUSEPOSITION res', res)
 flowStore.activeAtom.inputList.forEach((item) => {
 if (item.key === 'position_x' || item.key === 'position_y') {
 const val = item.key === 'position_x' ? x : y
 item.value = val
 createDom({ val }, item, ORIGIN_SPECIAL)
 }
 })
 })
 useFormPick('POINT')
 break
 default:
 break
 }
}

// 현재원자 기능의값
export function syncCurrentAtomData(itemData: RPA.AtomDisplayItem, flush = true, atomId?: string) {
 const flowStore = useFlowStore()
 const targetAtomId = atomId || flowStore.activeAtom?.id
 if (!targetAtomId) {
 return
 }
 const idx = flowStore.simpleFlowUIData.findIndex(item => item.id === targetAtomId)
 let alias = ''
 let flag = true
 if (itemData.key === 'anotherName') {
 flag = false
 alias = Array.isArray(itemData.value) ? itemData.value.reduce((cur, pre) => cur += pre.value, '') : itemData.value as string
 }
 if (itemData.groupId) {
 flowStore.setFormItemValue('anotherName', alias, backContainNodeIdx(itemData.groupId), flush)
 }
 flowStore.setFormItemValue(flag ? itemData.key : 'anotherName', flag ? itemData.value : alias, idx, flush)
}

function useRenderFormType() {
 // 관리유형보기, 사용자 지정대화상자계획대기팝업버튼관리
 function handleModalButton(itemData: RPA.AtomDisplayItem) {
 const { activeAtom } = useFlowStore()
 if (itemData.key === 'design_interface') {
 const title = getRealValue(activeAtom.inputList.find(item => item.key === 'box_title')?.value)
 NiceModal.show(CustomDialog, {
 title,
 option: itemData.value as string,
 onOk: (data) => {
 itemData.value = data
 syncCurrentAtomData(itemData)
 },
 })
 }
 else if (itemData.key === 'preview_button') {
 const { key, inputList } = activeAtom
 NiceModal.show(UserFormDialogModal, {
 option: { mode: 'modal', buttonType: 'confirm_cancel', ...getUserFormOption({ key, inputList }) },
 })
 }
 else if (itemData.key === 'contract_validate') {
 const flowStore = useFlowStore()
 const { activeAtom } = flowStore
 const { inputList } = activeAtom
 const target = inputList.find(item => item.key === 'custom_factors')
 // 전중단여부있음필요
 if (!target.value) {
 message.warning(i18next.t('atomForm.addElementFirst'))
 return
 }
 let hasProcessVarType = false // 여부있음프로세스변수
 const params = inputList.reduce((res, curr) => {
 res[curr.key] = getRealValue(curr.value, PROCESS_VAR_TYPE)
 if (res[curr.key].includes(PROCESS_VAR_TYPE)) {
 hasProcessVarType = true
 }
 return res
 }, {})
 if (hasProcessVarType) {
 message.warning(i18next.t('atomForm.hasProcessVarCannotValidate'))
 return
 }
 itemData.formType.params.loading = true
 validateContractResult(params).then((data) => {
 NiceModal.show(ContractValidateModal, { dataList: data })
 }).finally(() => {
 itemData.formType.params.loading = false
 })
 }
 else if (itemData.key === 'replace_table') {
 NiceModal.show(EmailTextReplaceModal, {
 option: itemData.value as string,
 onOk: (data) => {
 itemData.value = data
 syncCurrentAtomData(itemData)
 },
 })
 }
 }

 function handleTextareaModal(e: Event, itemData: RPA.AtomDisplayItem) {
 NiceModal.show(TextareaModal, {
 itemDataVal: itemData.value as RPA.AtomFormItemResult[],
 onOk: (data) => {
 itemData.value = data
 const target = e.target as HTMLElement
 const targetNode = (target.parentNode as HTMLElement).previousElementSibling as HTMLElement
 if (targetNode) {
 targetNode.innerHTML = String(generateInputVal(itemData))
 syncCurrentAtomData(itemData)
 }
 },
 })
 }

 async function handleHTMLContentPaste() {
 const { activeAtom } = useFlowStore()
 const { inputList } = activeAtom
 const is_html = inputList.find(item => item.key === 'is_html')?.value
 const data = await getHTMLClip({ is_html })
 return data?.content
 }

 return {
 handleModalButton,
 handleTextareaModal,
 handleHTMLContentPaste,
 }
}

function batchDone(itemData: RPA.AtomDisplayItem) {
 BUS.$off('batch-done')
 BUS.$on('batch-done', (res: any) => {
 console.log('on batch-done')
 itemData.value = [{ type: ELEMENT_IN_TYPE, value: res.value, data: res.data }]
 createDom({ val: res.value, elementId: res.data, category: ELEMENT_IN_TYPE }, itemData, ORIGIN_VAR)
 })
}

export function inputListListener(itemData: RPA.AtomDisplayItem, itemType: string) {
 if (itemData?.key === 'batch_data' && itemType === 'PICK') {
 batchDone(itemData)
 }
}

export default useRenderFormType
