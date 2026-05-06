import type { Ref } from 'vue'
import { nextTick } from 'vue'

// 추가또는동작점의경과중추가위치의dom
let listPlaceholder = null
export function draggableAddGhost(e, lastid) {
 console.log('draggableAddGhost', e, lastid)
 listPlaceholder = document.querySelector('.list-placeholder')
 const dom = document.querySelector('#listwrapper .sortable-ghost ') as HTMLElement
 console.log('draggableAddGhost dom', dom)

 // 수정방식, 추가위치dom
 if (lastid && lastid === e.related.dataset.id && listPlaceholder && e.willInsertAfter) {
 // 만약동작까지마지막일개점후, 이면위치점hover표시
 listPlaceholder.className += ' active'
 }
 else if (dom) {
 const indent = Number.parseInt(e.related.dataset.indent)
 dom.style.width = `calc(100% - ${indent}px)`
 dom.style.left = `${indent - 1}px`
 // let doms = ""; TODO
 // Array.from({ length: parseInt(e.related.dataset.parentids) }).forEach((i, idx) => {
 // doms += `<i key="sortable-ghostline${idx}" class="ghost-inner-dom ghostline" style="left:${
 // indent - idx * PAGE_LEVEL_INDENT - 2
 // }px"></i>`;
 // });
 // doms += `<i key="sortable-ghostborder" class="ghost-inner-dom ghostborder" style="width:calc(100% - ${indent}px);left:${indent}px"></i>`;
 // dom.innerHTML += doms;
 }
}

// 재방식, 삭제위치의dom
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
