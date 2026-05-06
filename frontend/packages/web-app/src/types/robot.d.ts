declare namespace RPA {
  interface RobotConfigItem {
    listKey: string
    title: string
    formItems: Array<RPA.AtomParam & { robotId: string, processId: string }>
  }

  /**
   * 컴포넌트
   */
  interface ComponentDetail {
    name: string // 컴포넌트이름
    icon: string // 컴포넌트아이콘
    latestVersion: number // 새버전
    creatorName: string // 생성사람
    introduction: string // 새버전의
    versionInfoList: Array<{
      version: number // 버전
      createTime: string // 생성 시간
      updateLog: string // 변경 로그
    }>
  }

  interface ComponentManageItem {
    componentId: string
    icon: string
    name: string
    introduction: string
    version: number
    blocked: number // 여부설치: 1 예 0 아니오 (`제거` 및 `설치` 버튼)
    isLatest: number // 여부예새버전: 1 예 0 아니오
    latestVersion: number // 새버전
  }

  /**
   * 데이터테이블
   */
  interface IDataTableSheets {
    active_sheet: string
    filename: string
    project_id: string
    sheets: IDataTableSheet[]
  }

  interface IDataTableSheet {
    name: string
    max_row: number
    max_column: number
    data: string[][]
  }

  interface IUpdateDataTableCell {
    sheet: string
    row: number
    col: number
    value: number | string | boolean
  }
}