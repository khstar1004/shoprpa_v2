import { applyPatches, enablePatches, produceWithPatches } from 'immer'
import type { Patch } from 'immer'
import { computed, ref, shallowRef } from 'vue'

enablePatches()

const MAX_HISTORY_LENGTH = 100

export interface UndoStackItem {
  patches: Patch[]
  inversePatches: Patch[]
}

export function useHistory<T>(baseState: T) {
  // 기록
  const history = shallowRef<UndoStackItem[]>([])
  // 현재검색
  const currentIndex = ref(-1)

  const state = shallowRef(baseState)

  // 실행기록
  const perform = (operation: (draft: T) => void | Promise<void> | T) => {
    const [newState, patches, inversePatches] = produceWithPatches(operation)(state.value)

    // 예결과상태있음변수, 직선연결반환
    if (patches.length === 0)
      return

    // 지우기현재검색후의기록
    history.value = history.value.slice(0, currentIndex.value + 1)

    // 추가새까지기록
    history.value.push({ patches, inversePatches })
    currentIndex.value++

    // 제한제어기록대소
    if (history.value.length > MAX_HISTORY_LENGTH) {
      history.value.shift()
      currentIndex.value--
    }

    state.value = newState
  }

  // 판매
  const undo = () => {
    if (!canUndo.value)
      return

    const { inversePatches } = history.value[currentIndex.value]
    state.value = applyPatches(state.value, inversePatches)
    currentIndex.value--
  }

  // 재
  const redo = () => {
    if (!canRedo.value)
      return

    currentIndex.value++
    const { patches } = history.value[currentIndex.value]
    state.value = applyPatches(state.value, patches)
  }

  // 조회여부가능으로판매
  const canUndo = computed<boolean>(() => {
    return currentIndex.value >= 0
  })

  // 조회여부가능으로재
  const canRedo = computed<boolean>(() => {
    return currentIndex.value < history.value.length - 1
  })

  return { canUndo, canRedo, state, perform, undo, redo }
}
