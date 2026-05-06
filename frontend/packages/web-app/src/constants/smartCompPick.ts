// 가능컴포넌트선택창통신파일
export enum SMART_COMP_PICK_EVENT {
  SHOW_MENU = 'show-menu', // 메뉴
  HIDE_MENU = 'hide-menu', // 메뉴
  PAUSE_PICK = 'pause-pick', // 일시중지선택
  RESUME_PICK = 'resume-pick', // 복사선택
  ZOOM_IN = 'zoom-in', // 대선택
  ZOOM_OUT = 'zoom-out', // 소선택
  CONFIRM = 'confirm', // 
  CANCEL = 'cancel', // 가져오기 
  SHOW_ERROR_DIALOG = 'show-error-dialog', // 오류대화상자
  ERROR_DIALOG_CONFIRM = 'error-dialog-confirm', // 오류대화상자
}