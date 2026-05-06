import { onBeforeUnmount, ref } from 'vue'

import { getKeyboard, resetKeyboard } from '@/utils/hotkeys'

import type { ShortcutItemMap } from '@/components/ShortcutInput/types.ts'

export function useShortcut(modelValue: ShortcutItemMap, emit) {
  const inputItem = ref(modelValue)

  const setInputItem = (data) => {
    inputItem.value = {
      ...inputItem.value,
      ...data,
    }
    emit('update:modelValue', inputItem.value)
    emit('change', inputItem.value)
    if (typeof (data.active) === 'boolean') {
      data.active ? emit('focus', inputItem.value) : emit('blur', inputItem.value)
    }
  }

  // 대기키보드입력
  const waitKeyboard = (e: Event) => {
    e.stopPropagation()
    const obj: any = { active: true }
    if (inputItem.value.text === '클릭') {
      obj.text = '입력'
    }
    setInputItem(obj)

    getKeyboard(inputItem.value, ({ text, value }) => {
      setInputItem({
        text,
        value,
      })
    })
  }

  // 닫기빠름입력
  const closeWaitKeyboard = (e: Event) => {
    e.stopPropagation()
    setInputItem({
      text: '클릭',
      active: false,
    })
    resetKeyboard()
  }

  // 완료빠름, 닫기
  const closeActiveKeyboard = (e: Event) => {
    e.stopPropagation()
    setInputItem({
      text: '입력새의',
      value: '',
      active: true,
    })
    getKeyboard(inputItem.value, ({ text, value }) => {
      setInputItem({
        text,
        value,
      })
    })
  }

  onBeforeUnmount(() => {
    resetKeyboard()
  })

  return {
    waitKeyboard,
    closeWaitKeyboard,
    closeActiveKeyboard,
    inputItem,
    setInputItem,
    resetKeyboard,
  }
}