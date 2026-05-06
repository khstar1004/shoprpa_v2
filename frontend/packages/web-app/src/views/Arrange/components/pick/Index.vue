<!-- 요소기기 -->
<script lang="ts" setup>
import {
 BorderOuterOutlined,
 CheckCircleOutlined,
 CloseOutlined,
 RedoOutlined,
 UnorderedListOutlined,
} from '@ant-design/icons-vue'
import { NiceModal } from '@rpa/components'
import { Image, message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { throttle } from 'lodash-es'
import { h, ref, toRaw, watch } from 'vue'

import { isBase64Image, trimBase64Header } from '@/utils/common'

import { getImageURL } from '@/api/http/env'
import { useElementsStore } from '@/stores/useElementsStore'
import { usePickStore } from '@/stores/usePickStore'
import type { PickElementType } from '@/types/resource.d'
import {
 CUSTOM_OPTIONS,
 CUSTOMIZATION,
 MATCH_OPTIONS,
 VISUALIZATION,
} from '@/views/Arrange/config/pick'
import {
 elementCustomFormat,
 elementCustomFormatRecover,
 elementDirectoryFormat,
 elementDirectoryFormatRecover,
} from '@/views/Arrange/utils/elementsUtils'

import CustomTable from './CustomTable.vue'
import DirectoryTable from './DirectoryTable.vue'
import PickForm from './PickForm.vue'

defineProps({
 isContinue: {
 type: Boolean,
 default: false,
 },
})

const modal = NiceModal.useModal()
const loading = ref('')
const singleLoading = ref('') // 개버튼loading상태
const useElements = useElementsStore()
const usePick = usePickStore()
const { t } = useTranslation()

const pickerType = ref('') // 선택유형
const similarButton = ref(false) // 여부가능으로선택요소
const similarCount = ref(0) // 요소수
const formOption = ref({
 pickName: '', // 선택요소이름
 editXPathType: VISUALIZATION, // xpath유형, 가능/사용자 지정/ 단일선택
 customOptions: CUSTOM_OPTIONS, // 유형[가능/사용자 지정]
 matchTypes: [], // 매칭유형 다중선택
 matchOptions: MATCH_OPTIONS, // 매칭방식
 pickType: 'web', // 선택유형
})
const pickFormRef = ref() // 테이블단일ref
const customData = ref([]) // 사용자 지정데이터
const nodeSourceData = ref([]) // 가능데이터
const detailElementData = ref<PickElementType>({
 // 요소
 app: '',
 version: '',
 type: 'uia',
 path: null,
})
const isShadow = ref(false) // 여부예요소

/**
 * 가져오기메시지
 */
function handleCancel() {
 // 다시 선택경과중아니오허용닫기팝업
 if (usePick.isPicking)
 return

 modal.hide()
 useElements.resetCurrentElement()
}

/**
 * 가져오기현재요소새데이터
 * 관리일부데이터형식
 */
function getLatestCurrentElementData(isSave: boolean) {
 const { version, type } = detailElementData.value
 let elementPathData
 let elementData
 if (type === 'web') {
 const customDataMap = toRaw(
 elementCustomFormatRecover(version, type, customData.value),
 ) // url, xpath, cssSelector
 const pathDirs = toRaw(
 elementDirectoryFormatRecover(version, type, nodeSourceData.value),
 ) // pathDirs
 elementPathData = {
 ...detailElementData.value.path,
 ...customDataMap,
 pathDirs,
 checkType: formOption.value.editXPathType,
 matchTypes: formOption.value.matchTypes,
 }
 if (isSave) {
 // 삭제아니오필요의데이터
 'img' in detailElementData.value && delete detailElementData.value.img
 'similarCount' in elementPathData && delete elementPathData.similarCount
 'rect' in elementPathData && delete elementPathData.rect
 }
 elementData = {
 ...detailElementData.value, // 요소외부데이터
 path: elementPathData, // 요소경로데이터
 }
 }
 else {
 elementPathData = elementDirectoryFormatRecover(
 version,
 type,
 nodeSourceData.value,
 ) // path 데이터
 const selfImg = useElements.currentElement.imageUrl
 const parentImg = useElements.currentElement.parentImageUrl
 // 저장시만약예base64이미지, 이면아니오저장base64
 const img = isSave
 ? {
 self: isBase64Image(selfImg) ? '' : selfImg,
 parent: isBase64Image(parentImg) ? '' : parentImg,
 }
 : {
 self: isBase64Image(selfImg) ? trimBase64Header(selfImg) : selfImg,
 parent: isBase64Image(parentImg)
 ? trimBase64Header(parentImg)
 : parentImg,
 }
 elementData = {
 ...detailElementData.value, // 요소데이터
 img,
 path: elementPathData,
 }
 if (elementData.picker_type === 'SIMILAR') {
 // uia 정도요소, 저장시변수변경 필드
 elementData.img = {
 self: isBase64Image(selfImg) ? '' : selfImg,
 parent: isBase64Image(parentImg) ? '' : parentImg,
 }
 elementData.similar_count ? delete elementData.similar_count : ''
 }
 }
 return elementData
}
/**
 * 계속선택
 */
function pickContinue() {
 usePick.newPick('', () => {
 loading.value = ''
 })
}
function saveBtnLoading(btn: string) {
 return loading.value === btn
}
function saveBtnDisabled() {
 return loading.value !== '' || usePick.isPicking || usePick.isChecking
}

/**
 * 저장요소
 */
const handleOk = throttle(
 async (saveContinue: boolean) => {
 const valid = await pickFormRef.value.validateName()
 if (!valid)
 return

 const elementData = getLatestCurrentElementData(true)
 const name = formOption.value.pickName.trim()
 if (name === '') {
 message.error(t('enterElementName'))
 return
 }

 if (useElements.checkName(name, useElements.currentElement.id)) {
 message.error(t('sameNameExists'))
 return
 }

 loading.value = saveContinue ? 'save_continue' : 'save'
 await useElements.saveElement(elementData, name, !saveContinue)
 loading.value = ''

 modal.hide()
 useElements.resetCurrentElement()
 // 계속열기시작선택
 saveContinue && pickContinue()
 },
 1500,
 { leading: true, trailing: false },
)

/**
 * 검증요소
 */
const handleValidateElement = throttle(
 () => {
 const elementData = getLatestCurrentElementData(false)
 console.log('검증 elementData: ', elementData)
 const element = JSON.stringify(elementData)
 usePick.startCheck(pickerType.value, element, (res) => {
 if (res.success)
 message.success(t('validationSuccess'))
 })
 },
 1500,
 { leading: true, trailing: false },
)

/**
 * 다시 선택
 */
const rePick = throttle(
 () => {
 singleLoading.value = 'rePick'
 const type = pickerType.value === 'SIMILAR' ? 'ELEMENT' : pickerType.value // 요소다시 선택, 필요자르기교체로 ELEMENT
 usePick.repick(type, false, '', () => {
 singleLoading.value = ''
 }) // 필요 없음팝업
 },
 1500,
 { leading: true, trailing: false },
)
/**
 * 요소 선택
 */
const similarPick = throttle(
 () => {
 singleLoading.value = 'similarPick'
 const elementData = getLatestCurrentElementData(false)
 usePick.similarPick(elementData, () => {
 singleLoading.value = ''
 })
 },
 1500,
 { leading: true, trailing: false },
)

//  useElements.currentElement 의변수업데이트표시
watch(
 () => useElements.currentElement,
 (newVal) => {
 if (newVal.elementData) {
 const eleData = JSON.parse(newVal.elementData)
 // console.log('eleData: ', eleData)
 const { version, type, path, picker_type, similar_count } = eleData
 // 현재요소이름
 formOption.value.pickName = newVal.name
 // 현재요소데이터
 detailElementData.value = eleData
 // 선택유형
 pickerType.value = picker_type
 // 사용자 지정요소
 customData.value = elementCustomFormat(version, type, path)
 // 선택버튼표시
 similarButton.value = ['web', 'uia'].includes(type)
 formOption.value.pickType = type
 // 만약예요소, 가져오기까지요소개데이터
 if (type === 'web') {
 const { checkType, matchTypes, shadowRoot } = path
 // 요소개데이터
 similarCount.value = path.similarCount ? path.similarCount : 0 // 요소개데이터
 // 요소표시유형, shadowRoot 로 true 시, 표시사용자 지정
 formOption.value.editXPathType = checkType || VISUALIZATION
 formOption.value.editXPathType = shadowRoot
 ? CUSTOMIZATION
 : formOption.value.editXPathType
 isShadow.value = shadowRoot
 formOption.value.customOptions = shadowRoot
 ? CUSTOM_OPTIONS.filter(item => item.value === CUSTOMIZATION)
 : CUSTOM_OPTIONS
 // 요소매칭유형
 formOption.value.matchTypes = matchTypes || []
 }
 else if (type === 'uia') {
 similarCount.value = picker_type === 'SIMILAR' ? similar_count : 0 // 요소개데이터
 formOption.value.customOptions = CUSTOM_OPTIONS.filter(
 item => item.value === VISUALIZATION,
 )
 }
 else {
 formOption.value.customOptions = CUSTOM_OPTIONS.filter(
 item => item.value === VISUALIZATION,
 )
 }
 // 가능요소
 nodeSourceData.value = elementDirectoryFormat(version, type, path)
 }
 },
 { immediate: true, deep: true },
)
</script>

<template>
 <a-modal
 v-bind="NiceModal.antdModal(modal)"
 destroy-on-close
 centered
 :width="730"
 :z-index="10"
 :title="$t('elementPicking')"
 class="pickModal"
 :keyboard="false"
 :mask-closable="false"
 >
 <template #closeIcon>
 <CloseOutlined
 :class="saveBtnDisabled() ? 'not-allowed' : ''"
 @click.stop="handleCancel"
 />
 </template>
 <template #footer>
 <a-button key="back" :disabled="saveBtnDisabled()" @click="handleCancel">
 {{ $t("cancel") }}
 </a-button>
 <a-button
 v-if="isContinue"
 key="save_continue"
 type="primary"
 :loading="saveBtnLoading('save_continue')"
 :disabled="saveBtnDisabled()"
 @click="
 () => {
 handleOk(true);
 }
 "
 >
 {{ $t("saveAndContinue") }}
 </a-button>
 <a-button
 key="save"
 type="primary"
 :loading="saveBtnLoading('save')"
 :disabled="saveBtnDisabled()"
 @click="handleOk(false)"
 >
 {{ $t("done") }}
 </a-button>
 </template>
 <div class="pickWrapper">
 <a-row class="pickWrapper-option">
 <a-col :span="5">
 <div
 class="pickWrapper-img border border-border mt-4 flex justify-center align-center"
 >
 <Image
 v-if="useElements.currentElement.imageUrl"
 :title="$t('fullSizeImage')"
 :src="getImageURL(useElements.currentElement.imageUrl)"
 />
 </div>
 </a-col>
 <a-col :span="6" class="pl-2">
 <div class="pickWrapper-buttons mt-4">
 <a-button
 size="small"
 :icon="h(RedoOutlined)"
 :loading="singleLoading === 'rePick' && usePick.isPicking"
 :disabled="usePick.isPicking || usePick.isChecking"
 class="font-size-12 inline-flex-center"
 @click="rePick"
 >
 {{ $t("rePickupElement") }}
 </a-button>
 </div>
 <div class="pickWrapper-buttons mt-2">
 <a-button
 class="font-size-12 inline-flex-center"
 :icon="h(BorderOuterOutlined)"
 size="small"
 :loading="usePick.isChecking"
 :disabled="usePick.isPicking"
 @click="handleValidateElement"
 >
 {{ $t("validateElement") }}
 </a-button>
 </div>
 <div class="pickWrapper-buttons mt-2">
 <a-button
 v-if="similarButton"
 size="small"
 :icon="h(UnorderedListOutlined)"
 :loading="singleLoading === 'similarPick' && usePick.isPicking"
 :disabled="usePick.isPicking || usePick.isChecking"
 class="font-size-12 inline-flex-center"
 @click="similarPick"
 >
 {{ $t("similarElementsPickup") }}
 </a-button>
 </div>
 <div v-if="similarCount" class="mt-1">
 <span class="similar-counts"><CheckCircleOutlined
 class="mr-2"
 style="color: #52c41a"
 />{{ $t('pickTips.foundSimilarCount', { count: similarCount }) }}</span>
 </div>
 </a-col>
 <a-col :span="13">
 <div class="pickWrapper-form mt-4">
 <PickForm ref="pickFormRef" :form-option="formOption" />
 </div>
 </a-col>
 </a-row>
 <div class="pickWrapper-table border-t border-b border-border mt-4">
 <div class="table-wapper">
 <div
 v-if="formOption.editXPathType === CUSTOMIZATION"
 :key="CUSTOMIZATION"
 class="px-2 fade-in"
 >
 <CustomTable :custom-data="customData" />
 </div>
 <div
 v-if="formOption.editXPathType === VISUALIZATION && !isShadow"
 :key="VISUALIZATION"
 class="px-2 fade-in"
 >
 <DirectoryTable :node-source="nodeSourceData" />
 </div>
 </div>
 </div>
 </div>
 </a-modal>
</template>

<style scoped lang="scss">
.font-size-12 {
 font-size: 12px;
}
.inline-flex-center {
 display: inline-flex;
 align-items: center;
}

.pickWrapper-table {
 height: 230px;
 overflow-y: auto;
}

.pickWrapper-table::-webkit-scrollbar {
 width: 6px;
 background-color: #f5f5f5;
}

.pickWrapper-table::-webkit-scrollbar-thumb {
 background-color: #cecece;
}

.pickWrapper-img {
 width: 120px;
 height: 120px;
 margin-left: 10px;
}

.similar-counts {
 font-size: 12px;
}

.not-allowed {
 cursor: not-allowed;
}

:deep(.ant-modal .ant-modal-content) {
 padding: 20px 12px;
}

:deep(.ant-image-preview-operations) {
 background: rgb(0 0 0 / 44%);
}

:deep(.ant-image-preview-root .ant-image-preview-mask) {
 background: rgb(255 255 255);
}

:deep(.pickWrapper-img .ant-image) {
 width: 100%;
 height: 100%;
 display: flex;
 justify-content: center;
 align-items: center;
}

:deep(.pickWrapper-img .ant-image .ant-image-img) {
 // max-width: 100%;
 width: auto;
 height: auto;
 max-height: 100px;
 max-width: 118px;
}
</style>
