import { nextTick } from 'vue'

import { ACTUATOR, APPLICATIONMARKET, DESIGNER, EDITORPAGE } from '@/constants/menu'
import router from '@/router'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import { addAtomData } from '@/views/Arrange/components/flow/hooks/useFlow'

export const WORKFLOW_EDITOR_SMOKE_QUERY_PARAM = '__shoprpa_workflow_smoke'
export const WORKFLOW_EDITOR_SMOKE_PROJECT_ID = 'shoprpa-workflow-smoke-project'
export const WORKFLOW_EDITOR_SMOKE_PROCESS_ID = 'shoprpa-workflow-smoke-main'
export const WORKFLOW_EDITOR_SMOKE_ATOM_KEY = 'Dialog.message_box'

const STORAGE_KEY = 'shoprpa.workflowEditorSmoke.v1'

type WorkflowEditorSmokePhase = 'create-save' | 'reload-edit'

interface WorkflowEditorSmokeProject {
  nextModuleId: number
  nextProcessId: number
  processNodes: Record<string, RPA.Atom[]>
  processes: RPA.Flow.ProcessModule[]
  revision: number
  updatedAt: string
}

interface WorkflowEditorSmokeDb {
  projects: Record<string, WorkflowEditorSmokeProject>
}

interface WorkflowEditorSmokeResult {
  activeProcessId: string
  flowNodeCount: number
  ok: boolean
  phase: WorkflowEditorSmokePhase
  projectId: string
  routeName: string
  savedNodeCount: number
  savedNodes: Array<{
    alias: string
    id: string
    key: string
    title: string
    version: string
  }>
  timestamp: string
}

declare global {
  interface Window {
    __SHOPRPA_RUN_WORKFLOW_SMOKE_PHASE__?: (phase: WorkflowEditorSmokePhase) => Promise<WorkflowEditorSmokeResult>
    __SHOPRPA_WORKFLOW_SMOKE_LAST_RESULT__?: WorkflowEditorSmokeResult | { error: string, ok: false, phase?: WorkflowEditorSmokePhase }
  }
}

const messageBoxAbility = {
  key: WORKFLOW_EDITOR_SMOKE_ATOM_KEY,
  title: '메시지 대화상자',
  version: '1.0.2',
  src: 'astronverse.dialog.dialog.Dialog().message_box',
  comment: '사용자에게 메시지 대화상자를 표시하고 선택한 버튼을 반환합니다.',
  inputList: [
    {
      types: 'Str',
      formType: { type: 'INPUT' },
      key: 'box_title',
      title: '대화상자 제목',
      name: 'box_title',
      default: '스모크 검증',
      required: false,
    },
    {
      types: 'Str',
      formType: { type: 'INPUT_VARIABLE_PYTHON' },
      key: 'message_content',
      title: '메시지 내용',
      name: 'message_content',
      default: '편집기 저장 검증 메시지',
      required: true,
    },
  ],
  outputList: [
    {
      types: 'Str',
      formType: { type: 'RESULT' },
      key: 'result_button',
      title: '선택한 버튼',
      tip: '',
    },
  ],
  icon: 'message-dialog',
  helpManual: '메시지 내용을 지정해 사용자 안내 대화상자를 표시합니다.',
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

function nowIso() {
  return new Date().toISOString()
}

function createDefaultProject(): WorkflowEditorSmokeProject {
  return {
    nextModuleId: 1,
    nextProcessId: 1,
    processNodes: {
      [WORKFLOW_EDITOR_SMOKE_PROCESS_ID]: [],
    },
    processes: [
      {
        resourceCategory: 'process',
        name: '프로세스',
        resourceId: WORKFLOW_EDITOR_SMOKE_PROCESS_ID,
      },
    ],
    revision: 0,
    updatedAt: nowIso(),
  }
}

function readDb(): WorkflowEditorSmokeDb {
  if (typeof window === 'undefined')
    return { projects: {} }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw)
      return { projects: {} }

    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object')
      return { projects: {} }

    return {
      projects: parsed.projects && typeof parsed.projects === 'object' ? parsed.projects : {},
    }
  }
  catch {
    return { projects: {} }
  }
}

function writeDb(db: WorkflowEditorSmokeDb) {
  if (typeof window === 'undefined')
    return

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(db))
}

function getProjectData(robotId = WORKFLOW_EDITOR_SMOKE_PROJECT_ID): WorkflowEditorSmokeProject {
  const db = readDb()
  if (!db.projects[robotId]) {
    db.projects[robotId] = createDefaultProject()
    writeDb(db)
  }
  return db.projects[robotId]
}

function updateProjectData(robotId: string, updater: (project: WorkflowEditorSmokeProject) => void) {
  const db = readDb()
  const project = db.projects[robotId] ?? createDefaultProject()
  updater(project)
  project.revision += 1
  project.updatedAt = nowIso()
  db.projects[robotId] = project
  writeDb(db)
  return project
}

function getCurrentProjectId() {
  return String(router.currentRoute.value.query.projectId || WORKFLOW_EDITOR_SMOKE_PROJECT_ID)
}

function currentRouteIsSmoke() {
  if (typeof window === 'undefined')
    return false

  const search = new URLSearchParams(window.location.search)
  return search.get(WORKFLOW_EDITOR_SMOKE_QUERY_PARAM) === '1'
}

export function isWorkflowEditorSmokeMode() {
  return currentRouteIsSmoke()
}

export function resetWorkflowEditorSmokeProject(robotId = WORKFLOW_EDITOR_SMOKE_PROJECT_ID) {
  const db = readDb()
  db.projects[robotId] = createDefaultProject()
  writeDb(db)
}

export function getWorkflowEditorSmokePermissions() {
  return [
    { resource: DESIGNER, actions: ['all'] },
    { resource: ACTUATOR, actions: ['all'] },
    { resource: APPLICATIONMARKET, actions: ['all'] },
    { resource: 'console', actions: ['all'] },
  ]
}

export function getWorkflowEditorSmokeAtomMeta(): RPA.AtomMetaData {
  return {
    atomicTree: [
      {
        key: 'dialog',
        title: '대화상자',
        icon: 'message-dialog',
        atomics: [
          {
            key: WORKFLOW_EDITOR_SMOKE_ATOM_KEY,
            title: '메시지 대화상자',
            icon: 'message-dialog',
          },
        ],
      },
    ],
    atomicTreeExtend: [],
    commonAdvancedParameter: [],
    types: {
      Any: {
        channel: 'global,main',
        template: '',
        desc: '전체',
        funcList: [],
        key: 'Any',
        src: '',
        version: '1.0.0',
      },
      Str: {
        channel: 'global,main',
        template: '',
        desc: '문자열',
        funcList: [],
        key: 'Str',
        src: '',
        version: '1.0.0',
      },
    },
  }
}

export function getWorkflowEditorSmokeAtomAbilityInfo(keys: string[]) {
  return keys
    .filter(key => key === WORKFLOW_EDITOR_SMOKE_ATOM_KEY)
    .map(() => JSON.stringify(messageBoxAbility))
}

export function getWorkflowEditorSmokeAtomTreeByParentKey(parentKey: string) {
  if (parentKey === WORKFLOW_EDITOR_SMOKE_ATOM_KEY || parentKey === 'dialog')
    return [JSON.stringify(messageBoxAbility)]

  return []
}

export function getWorkflowEditorSmokeProcessList(robotId: string): RPA.Flow.ProcessModule[] {
  return clone(getProjectData(robotId).processes)
}

export function getWorkflowEditorSmokeProcessNodes(robotId: string, processId: string): RPA.Atom[] {
  return clone(getProjectData(robotId).processNodes[processId] ?? [])
}

export function saveWorkflowEditorSmokeProcess(robotId: string, processId: string, nodes: RPA.Atom[]) {
  updateProjectData(robotId, (project) => {
    if (!project.processes.some(process => process.resourceId === processId)) {
      project.processes.push({
        resourceCategory: 'process',
        name: '프로세스',
        resourceId: processId,
      })
    }
    project.processNodes[processId] = clone(nodes)
  })
}

export function addWorkflowEditorSmokeProcess(robotId: string, processName: string) {
  const project = updateProjectData(robotId, (project) => {
    project.nextProcessId += 1
    const id = `shoprpa-workflow-smoke-process-${project.nextProcessId}`
    project.processes.push({
      resourceCategory: 'process',
      name: processName,
      resourceId: id,
    })
    project.processNodes[id] = []
  })

  return project.processes[project.processes.length - 1].resourceId
}

export function addWorkflowEditorSmokeModule(robotId: string, moduleName: string) {
  const project = updateProjectData(robotId, (project) => {
    project.nextModuleId += 1
    const id = `shoprpa-workflow-smoke-module-${project.nextModuleId}`
    project.processes.push({
      resourceCategory: 'module',
      name: moduleName,
      resourceId: id,
    })
    project.processNodes[id] = []
  })

  return project.processes[project.processes.length - 1].resourceId
}

export function genWorkflowEditorSmokeProcessName(robotId: string) {
  const project = getProjectData(robotId)
  return `새 하위 프로세스 ${project.nextProcessId + 1}`
}

export function genWorkflowEditorSmokeModuleName(robotId: string) {
  const project = getProjectData(robotId)
  return `새 Python 모듈 ${project.nextModuleId + 1}`
}

export function renameWorkflowEditorSmokeProcess(robotId: string, processId: string, name: string) {
  updateProjectData(robotId, (project) => {
    const process = project.processes.find(item => item.resourceId === processId)
    if (process)
      process.name = name
  })
}

export function deleteWorkflowEditorSmokeProcess(robotId: string, processId: string) {
  updateProjectData(robotId, (project) => {
    project.processes = project.processes.filter(item => item.resourceId !== processId)
    delete project.processNodes[processId]
  })
  return true
}

export function getWorkflowEditorSmokeDataTable(projectId: string): RPA.IDataTableSheets {
  return {
    active_sheet: 'Sheet1',
    filename: 'data_table',
    project_id: projectId,
    sheets: [
      {
        name: 'Sheet1',
        max_column: 0,
        max_row: 0,
        data: [],
      },
    ],
  }
}

function waitFor<T>(label: string, getter: () => T | Promise<T>, timeoutMs = 12000): Promise<T> {
  const started = Date.now()
  return new Promise((resolve, reject) => {
    const tick = async () => {
      try {
        const value = await getter()
        if (value) {
          resolve(value)
          return
        }
      }
      catch {
      }

      if (Date.now() - started > timeoutMs) {
        reject(new Error(`${label} timed out after ${timeoutMs}ms`))
        return
      }
      window.setTimeout(tick, 100)
    }
    tick()
  })
}

async function waitForEditorReady() {
  await router.isReady()
  const processStore = useProcessStore()
  const docStore = useProjectDocStore()

  await waitFor('workflow editor route', () => router.currentRoute.value.name === EDITORPAGE)
  await waitFor('workflow process load', () => {
    if (!processStore.activeProcessId)
      return false
    try {
      docStore.userFlowNode(processStore.activeProcessId)
      return true
    }
    catch {
      return false
    }
  })
}

function summarizeNodes(nodes: RPA.Atom[]) {
  return nodes.map(node => ({
    alias: node.alias,
    id: node.id,
    key: node.key,
    title: node.title,
    version: node.version,
  }))
}

async function runWorkflowEditorSmokePhase(phase: WorkflowEditorSmokePhase): Promise<WorkflowEditorSmokeResult> {
  if (!isWorkflowEditorSmokeMode())
    throw new Error('Workflow editor smoke mode is not enabled.')

  await waitForEditorReady()

  const projectId = getCurrentProjectId()
  const processStore = useProcessStore()
  const flowStore = useFlowStore()
  const docStore = useProjectDocStore()
  const activeProcessId = processStore.activeProcessId

  if (!activeProcessId)
    throw new Error('No active workflow process is loaded.')

  if (phase === 'create-save') {
    resetWorkflowEditorSmokeProject(projectId)
    docStore.clear()
    await nextTick()

    const created = await addAtomData(WORKFLOW_EDITOR_SMOKE_ATOM_KEY, 0, false)
    if (!created || flowStore.simpleFlowUIData.length < 1)
      throw new Error('Workflow smoke failed to add a message dialog node.')

    await processStore.saveProject()
  }
  else {
    if (!flowStore.simpleFlowUIData.some(node => node.key === WORKFLOW_EDITOR_SMOKE_ATOM_KEY))
      throw new Error('Saved workflow node was not reloaded into the editor.')

    const editedAlias = `스모크 수정 ${Date.now()}`
    flowStore.setFormItemValue('anotherName', editedAlias, 0)
    await nextTick()
    await processStore.saveProject()

    const savedAfterEdit = getWorkflowEditorSmokeProcessNodes(projectId, activeProcessId)
    if (savedAfterEdit[0]?.alias !== editedAlias)
      throw new Error('Edited workflow node alias was not persisted after save.')
  }

  const savedNodes = getWorkflowEditorSmokeProcessNodes(projectId, activeProcessId)
  if (savedNodes.length < 1)
    throw new Error('Workflow save produced no persisted nodes.')

  const result: WorkflowEditorSmokeResult = {
    activeProcessId,
    flowNodeCount: flowStore.simpleFlowUIData.length,
    ok: true,
    phase,
    projectId,
    routeName: String(router.currentRoute.value.name || ''),
    savedNodeCount: savedNodes.length,
    savedNodes: summarizeNodes(savedNodes),
    timestamp: nowIso(),
  }
  window.__SHOPRPA_WORKFLOW_SMOKE_LAST_RESULT__ = result
  return result
}

export function installWorkflowEditorSmokeHarness() {
  if (!isWorkflowEditorSmokeMode())
    return

  window.__SHOPRPA_RUN_WORKFLOW_SMOKE_PHASE__ = async (phase: WorkflowEditorSmokePhase) => {
    try {
      return await runWorkflowEditorSmokePhase(phase)
    }
    catch (error) {
      const failed = {
        error: error instanceof Error ? error.message : String(error),
        ok: false as const,
        phase,
      }
      window.__SHOPRPA_WORKFLOW_SMOKE_LAST_RESULT__ = failed
      throw error
    }
  }
}
