import { message } from 'ant-design-vue'
import hotkeys from 'hotkeys-js'

import i18next from '@/plugins/i18next'

import { SCOPE } from '@/constants/shortcuts'
import { useFlowStore } from '@/stores/useFlowStore'
import { batchToggleNode, copy, cut, debug, deleteAtomData, group, paste, runFromHere, ungroup } from '@/views/Arrange/components/flow/hooks/useFlow'
import { Group, GroupEnd } from '@/views/Arrange/config/atomKeyMap'
import type { ContextmenuInfo } from '@/views/Arrange/types/flow'
import { findPairId, getIdx, getMultiSelectIds } from '@/views/Arrange/utils/flowUtils'
import { setMultiSelectByClick, setSelectAll } from '@/views/Arrange/utils/selectItemByClick'

export function getContextMenuList() {
  return [
    {
      key: 'runHere',
      title: 'runFromHere',
      icon: 'tools-run',
      disable: (atom: RPA.Atom) => useFlowStore().multiSelect || atom.disabled || atom.level !== 1,
      disableTip: i18next.t('arrange.cannotRunFromHere'),
      clickFn: runFromHere,
      shortcutKey: 'Ctrl+Alt+H',
    },
    /*
 {
 key: 'recordingHere',
 title: 'startRecordingHere',
 clickFn: recordFromHere,
 shortcutKey: 'Ctrl+Alt+N',
 }, */
    {
      key: 'runDebug',
      title: 'runDebug',
      icon: 'tools-debug',
      disable: (atom: RPA.Atom) => useFlowStore().multiSelect || atom.disabled || atom?.key === Group || atom?.key === GroupEnd,
      disableTip: i18next.t('arrange.runDebugDisabledTip'),
      clickFn: debug,
      actionicon: 'tools-run',
      actionOper: true,
      shortcutKey: 'Ctrl+Alt+R',
    },
    {
      key: 'enableToggle',
      title: (atom: RPA.Atom) => atom.disabled ? 'enableAtom' : 'disableAtom',
      icon: 'tools-disabled',
      disable: false,
      clickFn: batchToggleNode,
      shortcutKey: 'Ctrl+B',
    },
    {
      type: 'divider',
    },
    {
      key: 'copy',
      title: 'copy',
      icon: 'tools-copy',
      disable: false,
      clickFn: copy,
      shortcutKey: 'Ctrl+C',
    },
    {
      key: 'cut',
      title: 'cut',
      icon: 'tools-cut',
      disable: false,
      clickFn: cut,
      shortcutKey: 'Ctrl+X',
    },
    {
      key: 'paste',
      title: 'paste',
      icon: 'tools-paste',
      disable: () => useFlowStore().multiSelect,
      disableTip: i18next.t('arrange.pasteDisabledTip'),
      clickFn: paste,
      shortcutKey: 'Ctrl+V',
    },
    {
      type: 'divider',
    },
    {
      key: 'mergeGroup',
      title: 'group',
      icon: 'tools-group',
      disable: false,
      clickFn: group,
      shortcutKey: 'Ctrl+G',
    },
    {
      key: 'unGroup',
      title: 'releaseGrouping',
      icon: 'tools-un-group',
      disable: (atom: RPA.Atom) => !(atom?.key === Group || atom?.key === GroupEnd),
      disableTip: i18next.t('arrange.ungroupDisabledTip'),
      clickFn: ungroup,
      shortcutKey: 'Ctrl+Shift+G',
    },
    {
      type: 'divider',
    },
    {
      key: 'deleteNode',
      title: 'deleteNode',
      disable: false,
      clickFn: deleteAtomData,
      icon: 'atom-delete',
      actionicon: 'atom-delete',
      actionOper: true,
      shortcutKey: 'Delete',
    },
    {
      key: 'selectAll',
      title: 'selectAll',
      disable: false,
      onlyShortcutKey: true,
      clickFn: setSelectAll,
      shortcutKey: 'Ctrl+A',
    },
  ]
}

// Right-click context menu instance.
let contextmenuRef = null
export function setContextMenu(contextRef) {
  contextmenuRef = contextRef
}

// Reposition the right-click menu inside the viewport.
export function toggleContextmenu(data: ContextmenuInfo) {
  if (contextmenuRef) {
    const contextMenuInfo: any = {
      visible: data.visible,
    }
    if (data.visible) {
      const menuHeight = 322
      const menuWidth = 190
      const viewportHeight = window.innerHeight
      const viewportWidth = window.innerWidth

      const clickX = data.$event.clientX
      const clickY = data.$event.clientY

      const spaceRight = viewportWidth - clickX
      if (spaceRight >= menuWidth + 10) {
        contextMenuInfo.x = clickX + 10
      }
      else {
        contextMenuInfo.x = Math.max(10, clickX - menuWidth - 10)
      }

      const spaceBelow = viewportHeight - clickY
      if (spaceBelow >= menuHeight) {
        contextMenuInfo.y = clickY
      }
      else {
        const yAbove = clickY - menuHeight
        contextMenuInfo.y = Math.max(10, yAbove)
      }

      contextMenuInfo.atom = data.atom

      const selectedAtomIds = useFlowStore().selectedAtomIds || []
      if (!selectedAtomIds.includes(data.atom.id)) {
        setMultiSelectByClick(data.atom, getIdx(data.atom.id), false, false)
      }
    }
    contextmenuRef.value = contextMenuInfo
  }
}

export function getDisabled(contextItem: any, atom?: RPA.Atom) {
  return typeof contextItem.disable === 'function' ? contextItem.disable(atom) : contextItem.disable
}

export function getTitle(contextItem: any, atom?: RPA.Atom) {
  return typeof contextItem.title === 'function' ? contextItem.title(atom) : contextItem.title
}

export function getSelected() {
  const activeAtom = useFlowStore().activeAtom
  const selectedAtomIds = useFlowStore().selectedAtomIds
  let selectedIds = []
  if (selectedAtomIds?.length > 0)
    selectedIds = selectedAtomIds
  else if (activeAtom)
    selectedIds = [activeAtom.id]
  return selectedIds
}

// 오른쪽 버튼메뉴돌아가기조정
export function clickContextItem(contextItem: any, atom?: RPA.Atom, from = 'contextMenu') {
  if (getDisabled(contextItem, atom))
    return message.warning(contextItem.disableTip)
  let atomIds = []
  // Group actions need the start/end pair.
  const specialContexts = ['unGroup', 'runHere', 'recordingHere']
  if (specialContexts.includes(contextItem.key)) {
    if (atom) {
      const { key, id } = atom
      // const index = useFlowStore().simpleFlowUIData.findIndex(i => i.id === id)
      if (contextItem.key === 'unGroup' && [Group, GroupEnd].includes(key)) {
        const nodeMap = useFlowStore().nodeContactMap
        const mapId = findPairId(nodeMap, id)
        atomIds = key === Group ? [id, mapId] : [mapId, id]
      }
      if (['runHere', 'recordingHere'].includes(contextItem.key)) {
        atomIds = [id]
      }
    }
  }
  else {
    atomIds = atom && from === 'action' ? getMultiSelectIds(atom.id) : getSelected()
  }
  if (atomIds.length === 0 && !['paste', 'selectAll'].includes(contextItem.key))
    message.warning(i18next.t('arrange.selectAtomFirst'))

  contextItem.clickFn && contextItem.clickFn(atomIds, atom)
}

/** Context-menu keyboard shortcuts. */
let isInListContainer = true
function clickFunc(e: MouseEvent) {
  isInListContainer = [...(e.composedPath() as any)].some((i) => {
    let cls = i.className || ''
    if (i.className && i.className.baseVal !== undefined) {
      // SVG elements expose className as SVGAnimatedString.
      cls = i.className.baseVal
    }
    return cls.includes('list-items-container') || cls.includes('link-select') || cls.includes('editor-tool') || i.id === 'listwrapper'
  })
}

export function enableContextMenuKeyboard(contextMenus) {
  document.onmousedown = clickFunc
  document.ondrop = clickFunc

  contextMenus.forEach((contextItem) => {
    const shortcutKey = contextItem.shortcutKey
    hotkeys.unbind(shortcutKey, SCOPE)
    hotkeys(shortcutKey, SCOPE, () => {
      if (isInListContainer) {
        clickContextItem(contextItem, useFlowStore().activeAtom)
      }
    })
  })
}
/**
 * Disable context-menu keyboard shortcuts.
 */
export function disableContextMenuKeyboard(contextMenus) {
  document.onmousedown = null
  contextMenus.forEach(({ shortcutKey }) => {
    hotkeys.unbind(shortcutKey, SCOPE)
  })
}
