// 기존가능닫기의일반량

// 기존유형
export enum ATOM_FORM_TYPE {
  INPUT = 'INPUT', // input유형
  SELECT = 'SELECT', // select유형
  CHECKBOX = 'CHECKBOX', // checkbox유형
  CHECKBOXGROUP = 'CHECKBOXGROUP', // checkboxgroup유형
  RADIO = 'RADIO', // radio유형
  SWITCH = 'SWITCH', // switch유형
  PYTHON = 'PYTHON', // python유형
  VARIABLE = 'VARIABLE', // var유형
  FILE = 'FILE', // file유형
  TEXTAREAMODAL = 'TEXTAREAMODAL', // 텍스트팝업유형
  ELEMENT = 'ELEMENT', // element유형
  DATETIME = 'DATETIME', // datetime유형
  COLOR = 'COLOR', // color유형
  PICK = 'PICK', // pick유형
  RESULT = 'RESULT', // result유형
  KEYBOARD = 'KEYBOARD', // keyboard유형
  FONTSIZENUMBER = 'FONTSIZENUMBER', // 문자숫자유형
  MODALBUTTON = 'MODALBUTTON', // 팝업버튼유형
  DEFAULTDATEPICKER = 'DEFAULTDATEPICKER', // 날짜선택기기
  RANGEDATEPICKER = 'RANGEDATEPICKER', // 날짜선택기기
  OPTIONSLIST = 'OPTIONSLIST', // 선택목록
  CV_IMAGE = 'CVIMAGE', // cv이미지유형
  CVPICK = 'CVPICK', // cvpick 유형
  GRID = 'GRID', // 구격식 유형
  SLIDER = 'SLIDER', // 구격식 유형
  DEFAULTPASSWORD = 'DEFAULTPASSWORD', // 통신비밀번호유형
  PROCESS_PARAM = 'PROCESSPARAM', // 구성 매개변수
  FACTORELEMENT = 'FACTORELEMENT', // 합치기필요
  CONTENTPASTE = 'CONTENTPASTE', // 내용붙여넣기
  MOUSEPOSITION = 'MOUSEPOSITION', // 마우스위치선택
  SCRIPTPARAMS = 'SCRIPTPARAMS', // 본매개변수관리관리
  REMOTEPARAMS = 'REMOTEPARAMS', // 매개변수
  REMOTEFOLDERS = 'REMOTEFOLDERS', // 폴더선택
  AIWORKFLOW = 'AIWORKFLOW', // 선택AI워크플로
}

// 저장까지py단말의유형
export const OTHER_IN_TYPE = 'other' // other유형
export const PY_IN_TYPE = 'python' // python유형
export const VAR_IN_TYPE = 'var' // var유형
export const GLOBAL_VAR_IN_TYPE = 'g_var' // 전역 변수유형
export const PARAMETER_VAR_IN_TYPE = 'p_var' // 구성 매개변수유형
export const ELEMENT_IN_TYPE = 'element' // element유형
export const CV_IN_TYPE = 'cv' // element유형
export const PROCESS_IN_TYPE = 'process' // process유형

// 기존가능입력출력변수유형
export enum VARIABLE_TYPE {
  BROWSER = 'Browser',
  EXCEL = 'ExcelObj',
  WORD = 'DocxObj',
}

// 변수선택기기중필요제한제어선택의변수유형
export const LIMIT_VARIABLE_SELECT = [VARIABLE_TYPE.BROWSER, VARIABLE_TYPE.EXCEL, VARIABLE_TYPE.WORD]