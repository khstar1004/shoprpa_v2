import http from './http'

/**
 * @description: 게시컴포넌트
 */
export function publishComponent(data: {
  componentId: string
  nextVersion: string
  updateLog: string
  name: string
  icon: string
  introduction: string
}) {
  return http.post('/api/robot/component-version/create', data)
}

/**
 * @description: 가져오기컴포넌트아래일개버전
 */
export function getComponentNextVersion(params: {
  componentId: string
}) {
  return http.get('/api/robot/component-version/next-version', params)
}

/**
 * @description: 를AI완료의코드변환성공가능컴포넌트기존가능정보
 */
export async function codeToMeta(data: { code: string }) {
  const res = await http.post('/scheduler/smart/code-to-meta', data)
  return res.data
}

/**
 * @description: 가능컴포넌트저장
 */
export async function saveSmartComp(data) {
  const res = await http.post('/api/robot/smart/save', data)
  return res.data.smartId as string
}

/**
 * @description: 가능컴포넌트가져오기
 */
export async function getSmartComp(data: { robotId: string, smartId: string }) {
  const res = await http.post<RPA.Atom>('/api/robot/smart/detail/all', data)
  return res.data
}

/**
 * @description: 
 */
export async function optimizeQuestion(data: {
  sceneCode: string
  user: string
  elements: any[]
}) {
  const res = await http.post<RPA.Atom>('/api/rpa-ai-service/smart/chat', { ...data, chatHistory: [] })
  return res.data?.choices?.[0]?.message?.content || ''
}
// 반환값예시: 
// ```new_prompt
// 에서`{요소_AI_8:1993287190512476160}`중완료가능요소의

// : 
// 1. 클릭`{요소_AI_8:1993287190512476160}`, 트리거AI공가능
// 2. 대기`{AI공가능}`열기또는로드완료
// 3. 에서열기의`{AI공가능}`중, 완료`[내용]`(예: 입력제목, 선택유형, 클릭완료버튼대기)

// 비고: 
// 1. 확인`{요소_AI_8:1993287190512476160}`가능클릭상태
// 2. 클릭후필요대기전체로드, 후실패
// 3. 요청 근거필요`[내용]`의
// ```