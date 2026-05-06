// 모든변수유형, 데이터연결입구반환
// eslint-disable-next-line import/no-mutable-exports
export let allVarType = {}
// 내보내기일개함수데이터, 사용변수유형
export function setVariableType(data = {}) {
 // 를전송입력의data매개변수 값에게allVarType변수, 만약data매개변수로빈, 이면값로빈대상
 allVarType = data || {}
}
