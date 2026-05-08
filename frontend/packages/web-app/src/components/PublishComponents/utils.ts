import type { Attachment } from '@/components/AttachmentUpload/index.vue'
import { DEFAULT_COLOR } from '@/constants/avatar'

export interface FormState {
  version: string | number // 버전
  updateLog: string // 변경 로그
  name: string // 이름
  icon: string // 아이콘
  color: string // 아이콘색상
  introduction: string //
  useDescription: string // 사용설명
  video: Attachment[] // 주소id
  appendix: Attachment[] // 파일주소id
  enableLastVersion: boolean // 여부사용해당버전
}

/**
 * 를백엔드저장의테이블단일변환로프론트엔드필요의형식
 */
export function toFrontData(data: Record<string, any> = {}): FormState {
  const { videoId, videoName, appendixId, appendixName, enableLastVersion } = data

  return {
    version: data.version,
    updateLog: '',
    name: data.name,
    icon: fromIcon(data.icon).icon,
    color: fromIcon(data.icon).color || DEFAULT_COLOR,
    introduction: data.introduction,
    useDescription: data.useDescription,
    video: videoId ? [{ uid: videoId, name: videoName, status: 'success' }] : [],
    appendix: appendixId ? [{ uid: appendixId, name: appendixName, status: 'success' }] : [],
    enableLastVersion: enableLastVersion === '1',
  }
}

/**
 * 를프론트엔드필요의형식변환로백엔드저장의형식
 */
export function toBackData(data: FormState) {
  return {
    version: data.version,
    updateLog: data.updateLog,
    name: data.name,
    icon: toIcon(data.icon, data.color),
    introduction: data.introduction,
    useDescription: data.useDescription,
    videoId: data.video[0]?.uid,
    videoName: data.video[0]?.name,
    appendixId: data.appendix[0]?.uid,
    appendixName: data.appendix[0]?.name,
    enableLastVersion: data.enableLastVersion ? '1' : '0',
  }
}

export function toIcon(icon: string, color: string) {
  return `${icon || ''}&color=${color || ''}`
}

export function fromIcon(iconAndColor: string) {
  const [icon, color] = iconAndColor?.split('&color=') || []
  return { icon, color }
}
