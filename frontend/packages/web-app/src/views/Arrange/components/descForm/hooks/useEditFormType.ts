import { ATOM_FORM_TYPE } from '@/constants/atom'

// 사용자 지정테이블단일정렬
export function useEditFormType() {
 const editItem = [
 {
 type: ATOM_FORM_TYPE.FILE,
 },
 {
 type: ATOM_FORM_TYPE.PICK,
 },
 {
 type: ATOM_FORM_TYPE.CVPICK,
 },
 {
 type: ATOM_FORM_TYPE.RADIO,
 },
 {
 type: ATOM_FORM_TYPE.SELECT,
 },
 {
 type: ATOM_FORM_TYPE.TEXTAREAMODAL,
 },
 {
 type: ATOM_FORM_TYPE.REMOTEPARAMS,
 },
 {
 type: ATOM_FORM_TYPE.REMOTEFOLDERS,
 },
 ]
 return { editItem }
}
