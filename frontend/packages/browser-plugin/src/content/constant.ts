export const MAX_TEXT_LENGTH = 10
export const MAX_TEXT_INCLUDE_LENGTH = 64
export const MAX_ATTRIBUTE_LENGTH = 32
export const DEEP_SEARCH_TRIGGER = 5 * 1000
export const ELEMENT_SEARCH_TRIGGER = 200
export const SCROLL_TIMES = 20
export const SCROLL_DELAY = 1500
export const HIGHT_BOX_SHADOW = 'inset 0px 0px 0px 2px red;'
export const HIGH_LIGHT_BG = '#ff4d4f85'
export const HIGH_LIGHT_BORDER = '2px solid red'
export const HIGH_LIGHT_COLOR = 'red'
export const HIGH_LIGHT_DURATION = 3000
export const ASTRON_SW_NAME = 'shoprpa-Service-Worker'
export enum StatusCode {
  SUCCESS = '0000',
  UNKNOWN_ERROR = '5001',
  ELEMENT_NOT_FOUND = '5002',
  EXECUTE_ERROR = '5003',
  VERSION_ERROR = '5004',
}

export enum ErrorMessage {
  ELEMENT_INFO_INCOMPLETE = '요소 정보가 완전하지 않아 요소를 찾을 수 없습니다.',
  ELEMENT_NOT_FOUND = '요소를 찾을 수 없습니다.',
  ELEMENT_MULTI_FOUND = '여러 요소가 일치하여 하나의 요소로 특정할 수 없습니다.',
  ELEMENT_NOT_INPUT = '선택한 요소는 입력 요소가 아닙니다.',
  ELEMENT_NOT_CHECKED = '선택한 요소에 체크 상태 속성이 없습니다.',
  ELEMENT_NOT_SELECT = '선택한 요소는 선택 가능한 요소가 아닙니다.',
  UNSUPPORT_ERROR = '지원하지 않는 기능입니다.',
  ELEMENT_PARENT_NOT_FOUND = '상위 요소를 찾을 수 없습니다. 요소가 아직 존재하는지 확인하세요.',
  ELEMENT_CHILD_NOT_FOUND = '하위 요소를 찾을 수 없습니다. 요소가 아직 존재하는지 확인하세요.',
  ELEMENT_CHILD_ORIGIN_NOT_FOUND = '기존 요소 정보에 originXpath가 없어 하위 요소를 특정할 수 없습니다.',
  UPDATE_TIP = '확장을 최신 버전으로 업데이트한 뒤 다시 시도하세요.',
}

export const SVG_NODETAGS = [
  'svg',
  'g',
  'defs',
  'symbol',
  'use',
  'image',
  'switch',
  'a',
  'text',
  'tspan',
  'textPath',
  'foreignObject',
  'rect',
  'circle',
  'ellipse',
  'line',
  'polyline',
  'polygon',
  'path',
  'animate',
  'animateMotion',
  'animateTransform',
  'set',
  'linearGradient',
  'radialGradient',
  'pattern',
  'clipPath',
  'mask',
  'filter',
  'feBlend',
  'feColorMatrix',
  'feComponentTransfer',
  'feComposite',
  'feConvolveMatrix',
  'feDiffuseLighting',
  'feDisplacementMap',
  'feFlood',
  'feGaussianBlur',
  'feImage',
  'feMerge',
  'feMorphology',
  'feOffset',
  'feSpecularLighting',
  'feTile',
  'feTurbulence',
  'feDistantLight',
  'fePointLight',
  'feSpotLight',
  'marker',
  'view',
  'metadata',
  'title',
  'desc',
]
