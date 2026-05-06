import { provide, ref } from 'vue'

function useArrangeProvide() {
 provide('showAtomFormItem', ref(true)) // 표시표시원자 기능
 provide('showAtomConfig', ref(false)) // 오른쪽원자 기능매칭테이블단일
}

export default useArrangeProvide
