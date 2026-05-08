import type { Ref } from 'vue'
import { nextTick } from 'vue'

// Placeholder used while dragging workflow nodes.
let listPlaceholder = null
export function draggableAddGhost(e, lastid) {
  listPlaceholder = document.querySelector('.list-placeholder')
  const dom = document.querySelector('#listwrapper .sortable-ghost ') as HTMLElement

  if (lastid && lastid === e.related.dataset.id && listPlaceholder && e.willInsertAfter) {
    listPlaceholder.className += ' active'
  }
  else if (dom) {
    const indent = Number.parseInt(e.related.dataset.indent)
    dom.style.width = `calc(100% - ${indent}px)`
    dom.style.left = `${indent - 1}px`
  }
}

// Clear the drag placeholder styles.
export function draggableDelGhost() {
  const dom = document.querySelector('.ghostborder') as HTMLElement
  if (dom) {
    dom.className = dom.className.replace(/ghostborder|ghostline\d+/g, '').replace(/ghostline/g, '').trim()
    dom.removeAttribute('style')
    listPlaceholder = document.querySelector('.list-placeholder')
    if (listPlaceholder) {
      listPlaceholder.className = 'list-placeholder'
    }
    listPlaceholder = null
  }
}

export function clearDraggable(draggableRef: Ref) {
  nextTick(() => {
    if (draggableRef.value) {
      if (draggableRef.value?._sortable?.el) {
        draggableRef.value._sortable.el = null
      }
      draggableRef.value._sortable = null
    }
  })
}
