declare namespace RPA {
  namespace Flow {
    type ProcessModuleType = 'process' | 'module'

    interface ProcessModule {
      resourceCategory: ProcessModuleType
      name: string
      resourceId: string
      isOpen?: boolean
      isSaveing?: boolean
      isMain?: boolean // 여부예프로세스
      isLoading?: boolean // 여부정상에서로드
    }

    interface Process {
      id: string
      name: string
      description: string
      code: string
      version: number
      robotId: string
      createTime: string
      updateTime: string
    }

    interface Code {
      id: string
      name: string
      description: string
    }
  }

  // 구성 매개변수
  interface ConfigParamData {
    id: string
    varDirection: 0 | 1 // 입력 / 출력
    varName: string // 매개변수이름
    varType: RPA.VariableType // 매개변수유형
    varValue: unknown // 매개변수 값
    varDescribe: string // 매개변수설명
    robotId: string // 사용id
    robotVersion?: number // 사용버전
    processId?: string // 프로세스id
    moduleId?: string // 모듈id
  }

  // 컴포넌트속성
  interface ComponentAttrData extends ConfigParamData {
    varFormType: {
      type: string
      value: any[]
    }
  }

  // 생성구성 매개변수
  type CreateConfigParamData = Omit<ConfigParamData, 'id'>

  // 생성컴포넌트속성
  type CreateComponentAttrData = Omit<ComponentAttrData, 'id'>

  // 전역 변수
  interface GlobalVariable {
    varName: string
    varType: string
    varValue: string
    varDescribe: string
    globalId?: string
    robotId?: string
    projectId?: string
  }

  type RunMode = 'PROJECT_LIST' | 'EXECUTOR' | 'CRONTAB' | 'EDIT_PAGE'
}
