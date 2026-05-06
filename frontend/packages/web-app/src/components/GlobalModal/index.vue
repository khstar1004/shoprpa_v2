<!--
 목록전목록중팝업필요있음삼방식: 
1. 에서필요사용 <a-modal></a-modal> 의방법아니오사용수정예전의사용법, 없음알림;
2. 에서필요사용 <Modal></Modal> 의방법, 입력 GlobalModal/index.vue, 완료<Global></Global>;
3. 에서필요사용 유형Modal.confirm({...}) 의방법, 입력 GlobalModal/index.ts, 완료GlobalModal.confirm({...});
삼방식매칭centered:true,keyborad:false;예결과후출력필요수정의, 가능행매칭개속성덮어쓰기매칭
-->
<script lang="ts" setup>
import type { ModalProps } from 'ant-design-vue'
import { Modal } from 'ant-design-vue'

// 수정로런타임 props , 에서 TSX 중유형변경지정
const props = defineProps({
  //  Modal 의모든 props
  ...({} as ModalProps),
  // 허용작업금액외부속성
} as {
  [K in keyof ModalProps]?: ModalProps[K]
} & {
  [key: string]: any
})
</script>

<template>
  <Modal
    class="global-modal"
    :keyboard="false"
    :centered="true"
    v-bind="props"
  >
    <template v-for="slotName in Object.keys($slots)" :key="slotName" #[slotName]="slotData">
      <slot
        :name="slotName"
        v-bind="slotData && typeof slotData === 'object' ? slotData : {}"
      />
    </template>
  </Modal>
</template>

<style lang="scss">
/* 시스템일header및footer의외부가장자리거리, content의가장자리거리열기발송의시UI */
.global-modal {
  .ant-modal-header {
    margin: 0 0 16px 0;
  }
  .ant-modal-footer {
    margin: 16px 0 0 0;
  }
}
</style>