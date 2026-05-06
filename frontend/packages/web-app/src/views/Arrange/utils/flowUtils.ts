import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import { Catch, CvImageExist, ForBrowserSimilar, ForDataTableLoop, ForDict, ForExcelContent, ForList, ForStep, Group, If, Try, TryEnd, While } from '@/views/Arrange/config/atomKeyMap'
import { ERR_PARENT_NOT_CONTAINS_ALL_CHILD } from '@/views/Arrange/config/errors'

/**
 * 가져오기배열중및현재점일단계의점
 * @param curItem 현재점
 * @param lastId 점id
 * @returns 단계목록Ids
 */
export function getCommonLevel(curItem: RPA.Atom, lastId: string) {
 const flowStore = useFlowStore()
 const findItem = flowStore.simpleFlowUIData.find(i => i.id === lastId)
 if (findItem) {
 return findItem.level === curItem.level
 }
}

export function backContainNodeIdx(idOrIndex: string | number) {
 const flowStore = useFlowStore()
 const nodeMap = flowStore.nodeContactMap
 let findId = ''
 if (typeof idOrIndex === 'number')
 idOrIndex = flowStore.simpleFlowUIData[idOrIndex].id
 for (const key in nodeMap) {
 if (Object.prototype.hasOwnProperty.call(nodeMap, key)) {
 const element = nodeMap[key]
 if (idOrIndex === key) {
 findId = element
 break
 }
 if (idOrIndex === element) {
 findId = key
 break
 }
 }
 }
 return flowStore.simpleFlowUIData.findIndex(i => i.id === findId)
}

export function generateContactIds() {
 const nodeMap = useFlowStore().nodeContactMap
 const startKeys = Object.keys(nodeMap)
 const endKeys = startKeys.map(key => nodeMap[key])
 return {
 startKeys,
 endKeys,
 contactMap: nodeMap,
 }
}

export function getIdx(id: string) {
 return useFlowStore().simpleFlowUIData.findIndex(i => i.id === id)
}

/**
 * 가져오기 개목록의모든데이터
 * @param first 첫 번째개점idx
 * @param second 이개점idx
 * @param arr 배열
 * @returns 개목록의점id
 */
export function betweenTowItem(first, second, arr) {
 const firstIdx = Math.min(first, second)
 const secondIdx = Math.max(first, second)
 return arr.slice(firstIdx, secondIdx + 1)
}

export function isContinuous(arr: number[]) {
 const set = new Set(arr)
 return set.size === arr.length && Math.max(...set) - Math.min(...set) + 1 === arr.length
}

export function getProjectAllFlow() {
 const processStore = useProcessStore()
 const allFlowList = {}
 processStore.processList.filter(i => i.resourceCategory !== 'module').forEach((item) => {
 allFlowList[item.resourceId] = useProjectDocStore().userFlowNode(item.resourceId)
 })
 return { allFlowList }
}

// 가져오기프로세스데이터종현재선택의점, 닫기 점및그점
export function getMultiSelectIds(id: string) {
 if (!id)
 return []
 const { startKeys, endKeys, contactMap } = generateContactIds()

 let currentIds = [id]

 let startId = ''
 let endId = ''
 // // 현재예세트유형의점
 if (startKeys.includes(id)) {
 startId = id
 endId = contactMap[id]
 }

 // 현재예세트유형의결과점
 if (endKeys.includes(id)) {
 startId = startKeys[endKeys.findIndex(endId => endId === id)]
 endId = id
 }

 // 일개결과점가능대상다중개점, 출력결과점대상의모든의열기 점
 const relatedStartKeys = [Catch, TryEnd].includes(useProjectDocStore().userFlowNode().find(n => n.id === id).key) ? startKeys.filter(key => contactMap[key] === endId) : []

 const allIds = new Set([startId, ...relatedStartKeys, endId].filter(i => i))

 if (allIds.size >= 2) {
 // 까지모든닫기 점의및결과검색
 const allIdx = [...allIds].map(i => useProjectDocStore().userFlowNode().findIndex(n => n.id === i))
 const minIdx = Math.min(...allIdx)
 const maxIdx = Math.max(...allIdx)
 currentIds = useProjectDocStore().userFlowNode().slice(minIdx, maxIdx + 1).map(i => i.id)
 }
 return currentIds
}

/**
 * 중단점여부로세트점(필요조회점).
 *
 * @param key 점의 key
 * @returns 여부로세트점
 */
function isComplexNode(key: string): boolean {
 return [
 Group,
 ForStep,
 ForDict,
 ForList,
 ForExcelContent,
 ForBrowserSimilar,
 ForDataTableLoop,
 While,
 If,
 Try,
 CvImageExist,
 ].includes(key)
}

/**
 * 조회점여부전체패키지에서선택중점중.
 *
 * @param childIds 점 ID 배열
 * @param selectedIds 선택중점 ID 배열
 * @returns 여부전체패키지
 */
function areAllChildrenSelected(childIds: string[], selectedIds: string[]): boolean {
 return childIds.every(id => selectedIds.includes(id))
}

/**
 * 필터링완료관리의점 ID.
 *
 * @param currentIds 현재선택중점 ID 배열
 * @param processedIds 완료관리의점 ID 배열
 * @returns 필터링후의점 ID 배열
 */
function filterProcessedIds(currentIds: string[], processedIds: string[]): string[] {
 return currentIds.filter(id => !processedIds.includes(id))
}

/**
 * 조회선택중의점여부가득으로아래건파일: 모든점패키지에서선택중점중
 * @param atomIds 선택중의점 ID 배열
 * @returns 오류정보배열
 */
export function validateSelectedNodes(atomIds: string[]): string[] {
 const flowStore = useFlowStore()
 const allNodes = flowStore.simpleFlowUIData
 let currentIds = [...atomIds] // 복사선택중점 ID 배열,
 const errors: string[] = []

 // 모든점, 조회선택중점의단계및점
 for (const node of allNodes) {
 if (!currentIds.includes(node.id))
 continue

 // 조회현재점단계점여부전체선택중
 const childIds = getMultiSelectIds(node.id)
 if (isComplexNode(node.key) && !areAllChildrenSelected(childIds, atomIds)) {
 errors.push(ERR_PARENT_NOT_CONTAINS_ALL_CHILD)
 console.log('선택원자 기능저장에서미완료선택의단계')
 break
 }

 // 필터링완료관리의점, 적음계획량
 currentIds = filterProcessedIds(currentIds, childIds)
 }

 return errors
}

/**
 * 근거 id 또는 endid 조회대상의 endid 또는 id
 * @param mapObj 예 {id: endid, id2: endid2} 의대상
 * @param value 필요조회의 id 또는 endid
 * @returns 대상의 endid 또는 id, 아니오까지반환 undefined
 */
export function findPairId(mapObj: Record<string, string>, value: string): string | undefined {
 // 정상에조회
 if (mapObj[value])
 return mapObj[value]
 // 반대에조회
 const entry = Object.entries(mapObj).find(([_k, v]) => v === value)
 return entry ? entry[0] : undefined
}
