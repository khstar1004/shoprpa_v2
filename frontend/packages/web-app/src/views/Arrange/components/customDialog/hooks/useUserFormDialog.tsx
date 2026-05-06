import type { FormInstance } from 'ant-design-vue'
import { Button } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { ref, shallowRef } from 'vue'

import type { AnyObj } from '@/types/common'
import type { DialogOption } from '@/views/Arrange/components/customDialog/types'

export default function useUserFormDialog(option: DialogOption, onClose: () => void, onSave?: (data: AnyObj) => void) {
 const { t } = useTranslation()
 // 지정테이블단일사용
 const formRef = shallowRef<FormInstance>(null)
 // 지정테이블단일상태
 const formState = ref(option.formModel)

 const handleClose = () => {
 if (option.mode !== 'modal') {
 // 사용자 지정대화상자클릭×닫기시필요출력{ result_button: 'cancel' }
 onSave?.({ result_button: 'cancel' })
 }

 onClose()
 }

 const handleBtns = async (btnOpt: string) => {
 // 필요예보기팝업있음작업업무서비스직선연결닫기
 if (option.mode !== 'modal') {
 const itemList = option.itemList
 if (itemList.length === 1 && itemList[0].dialogFormType === 'MESSAGE_CONTENT') {
 formState.value[itemList[0].bind] = btnOpt
 onSave?.(formState.value)
 }
 else if (btnOpt === 'confirm') {
 await formRef.value.validate()
 formState.value.result_button = btnOpt
 onSave?.(formState.value)
 }
 else if (btnOpt === 'cancel') {
 onSave?.({ result_button: 'cancel' })
 }
 }

 onClose()
 }

 const renderFooterBtns = (buttonType: string) => {
 const buttons = {
 confirm: <Button type="primary" onClick={() => handleBtns('confirm')}>{t('confirm')}</Button>,
 cancel: <Button onClick={() => handleBtns('cancel')}>{t('cancel')}</Button>,
 yes: <Button type="primary" onClick={() => handleBtns('yes')}>{t('yes')}</Button>,
 no: <Button onClick={() => handleBtns('no')}>{t('no')}</Button>,
 }

 return (
 <>
 {buttonType.split('_').reverse().map(item => buttons[item])}
 </>
 )
 }

 return {
 formRef,
 formState,
 handleClose,
 renderFooterBtns,
 }
}
