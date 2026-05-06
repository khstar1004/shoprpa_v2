import { ATOM_FORM_TYPE } from '@/constants/atom'
import { getRealValue } from '@/views/Arrange/components/atomForm/hooks/usePreview'

// 지정테이블단일정렬
export function useFormItemSort() {
  const editItem = [
    {
      type: ATOM_FORM_TYPE.PYTHON,
    },
    {
      type: ATOM_FORM_TYPE.INPUT,
    },
    {
      type: ATOM_FORM_TYPE.ELEMENT,
    },
    {
      type: ATOM_FORM_TYPE.CV_IMAGE,
    },
    {
      type: ATOM_FORM_TYPE.DATETIME,
    },
    {
      type: ATOM_FORM_TYPE.COLOR,
    },
    {
      type: ATOM_FORM_TYPE.FILE,
    },
    {
      type: ATOM_FORM_TYPE.TEXTAREAMODAL,
    },
    {
      type: ATOM_FORM_TYPE.VARIABLE,
    },
    {
      type: ATOM_FORM_TYPE.REMOTEFOLDERS,
    },
  ]
  const extraItem = [
    {
      type: ATOM_FORM_TYPE.PICK,
    },
    {
      type: ATOM_FORM_TYPE.CVPICK,
    },
    {
      type: ATOM_FORM_TYPE.GRID,
    },
    {
      type: ATOM_FORM_TYPE.SLIDER,
    },
    {
      type: ATOM_FORM_TYPE.CHECKBOX,
    },
    {
      type: ATOM_FORM_TYPE.CHECKBOXGROUP,
    },
    {
      type: ATOM_FORM_TYPE.RADIO,
    },
    {
      type: ATOM_FORM_TYPE.SELECT,
    },
    {
      type: ATOM_FORM_TYPE.SWITCH,
    },
    {
      type: ATOM_FORM_TYPE.KEYBOARD,
    },
    {
      type: ATOM_FORM_TYPE.FONTSIZENUMBER,
    },
    {
      type: ATOM_FORM_TYPE.MODALBUTTON,
    },
    {
      type: ATOM_FORM_TYPE.DEFAULTDATEPICKER,
    },
    {
      type: ATOM_FORM_TYPE.RANGEDATEPICKER,
    },
    {
      type: ATOM_FORM_TYPE.OPTIONSLIST,
    },
    {
      type: ATOM_FORM_TYPE.DEFAULTPASSWORD,
    },
    {
      type: ATOM_FORM_TYPE.PROCESS_PARAM,
    },
    {
      type: ATOM_FORM_TYPE.FACTORELEMENT,
    },
    {
      type: ATOM_FORM_TYPE.CONTENTPASTE,
    },
    {
      type: ATOM_FORM_TYPE.MOUSEPOSITION,
    },
    {
      type: ATOM_FORM_TYPE.SCRIPTPARAMS,
    },
    {
      type: ATOM_FORM_TYPE.AIWORKFLOW,
    },
    {
      type: ATOM_FORM_TYPE.REMOTEPARAMS,
    },
  ]
  return { extraItem, editItem }
}

// 테이블단일여부
export function useFormItemRequired(item: RPA.AtomDisplayItem) {
  const { required, value: atomValue } = item
  if (!required)
    return required
  if (Array.isArray(atomValue)) {
    return atomValue.every(atomItem => Object.is(atomItem.value, ''))
  }
  if (typeof atomValue === 'boolean')
    return false
  if (atomValue === '')
    return true
  return false
}

// 테이블단일길이정도제한제어안내
export function getLimitLengthTip(limitLength: Array<string | number>) {
  const [min, max] = limitLength
  if (['-1', 1].includes(min)) {
    return `${max}자 이하여야 합니다.`
  }
  if (['-1', 1].includes(max)) {
    return `${min}자 이상이어야 합니다.`
  }
  return `${min}자 이상 ${max}자 이하여야 합니다.`
}

// 테이블단일여부기호합치기길이정도제한제어
export function useFormItemLimitLength(item: RPA.AtomDisplayItem) {
  const { limitLength, value } = item
  if (!(limitLength && limitLength.length === 2))
    return true
  const atomValue = getRealValue(value)
  const [min, max] = limitLength // [-1, 16] 있음대길이정도제한제어 [4, -1] 있음소길이정도제한제어 [4, 16] 있음소대길이정도제한제어
  if (['-1', 1].includes(min)) {
    return atomValue.length <= max
  }
  if (['-1', 1].includes(max)) {
    return atomValue.length >= min
  }
  return atomValue.length >= min && atomValue.length <= max
}
