import { isEmpty, omit } from 'lodash-es'

import { translate } from '@/plugins/i18next'

export interface AtomTreeNode extends Omit<RPA.AtomTreeNode, 'atomics'> {
 uniqueId: string
 atomics?: AtomTreeNode[]
}

/**
 * addUniqueIdsToTree 함수데이터: 로중의매개점추가일의 ID.
 *
 * @param {RPA.AtomTreeNode} node 현재정상에서관리의점(또는의점).
 * @param {string} path 현재점까지점의경로(사용완료 ID).
 * @returns {AtomTreeNode} 반환완료수정의점 (추가완료 ID). 비고: 반환의예일개새의점대상, 아니오예직선연결수정전송입력의 node.
 */
export function addUniqueIdsToTree(node: RPA.AtomTreeNode, path: string = ''): AtomTreeNode {
 // 현재점의 key 및점의 path 생성새의경로.
 const newPath = path ? `${path}/${node.key}` : node.key

 // 생성현재점의일개얕음, 그리고를새경로로그 ID.
 const newNode: AtomTreeNode = {
 ...omit(node, 'atomics'),
 uniqueId: newPath,
 }

 // 만약현재점있음점, 이면로매개점추가 ID.
 if (node.atomics) {
 newNode.atomics = node.atomics.map(child => addUniqueIdsToTree(child, newPath))
 }

 // 반환수정후의점 (얕음), 예추가완료 id 속성의점.
 return newNode
}

/**
 * 에서결과중검색제목패키지닫기키의점, 그리고반환일개새의결과, 패키지매칭의점및의.
 * @param treeNodes 기존의점배열
 * @param keyword 필요검색의닫기키
 * @returns 일개새의결과, 패키지매칭닫기키의점및의
 */
export function searchTreeAndKeepStructure(treeNodes: AtomTreeNode[], keyword: string): [AtomTreeNode[], string[]] {
 const expandKeys: string[] = []

 function traverse(nodes: AtomTreeNode[] | undefined): AtomTreeNode[] | undefined {
 if (!nodes) {
 return undefined
 }

 const filteredNodes: Array<AtomTreeNode> = []

 for (const node of nodes) {
 let matchedChildren: AtomTreeNode[] | undefined
 if (node.atomics) {
 matchedChildren = traverse(node.atomics)
 }

 const isMatch = translate(node.title).toLowerCase().includes(keyword.toLowerCase())

 if (matchedChildren?.length > 0) {
 expandKeys.push(node.uniqueId)
 }

 if (isMatch || matchedChildren?.length > 0) {
 filteredNodes.push({
 ...node,
 atomics: isEmpty(matchedChildren) ? node.atomics : matchedChildren,
 })
 }
 }

 return filteredNodes.length > 0 ? filteredNodes : undefined
 }

 return [traverse(treeNodes) || [], expandKeys]
}
