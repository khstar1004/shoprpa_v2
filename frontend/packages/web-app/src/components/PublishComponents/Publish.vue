<script setup lang="ts">
import { Button, Checkbox, message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import type { PropType } from 'vue'
import { computed, defineAsyncComponent, ref, useTemplateRef } from 'vue'

import { releaseWithPublish } from '@/api/market'
import { publishRobot } from '@/api/robot'
import { useCommonOperate } from '@/views/Home/pages/hooks/useCommonOperate'

import type BasicForm from './BasicForm.vue'
import type { FormState } from './utils'
import { toBackData } from './utils'

const props = defineProps({
  robotId: {
    type: String,
    required: true,
  },
  defaultData: Object as PropType<FormState>,
})

const emits = defineEmits(['submited'])
const BasicFormComponent = defineAsyncComponent(() => import('./BasicForm.vue'))
const { t } = useTranslation()
const { applicationReleaseCheck } = useCommonOperate()

const isFirstVerison = computed(() => props.defaultData?.version === 1)

const basicFormData = ref<FormState>(props.defaultData)
const basicFormRef
  = useTemplateRef<InstanceType<typeof BasicForm>>('basicForm')
const enableLastVersion = ref(isFirstVerison.value)

async function handleSubmit(): Promise<void> {
  await basicFormRef.value.validate()

  const lastPublishData = {
    robotId: props.robotId,
    ...toBackData({
      ...basicFormData.value,
      enableLastVersion: enableLastVersion.value,
    }),
  }
  console.log('lastPublishData', lastPublishData)

  const res = await publishRobot(lastPublishData)
  message.success(t('publish.success'))

  // 조회여부필요위신청(예결과열기시작위검토공유경과마켓, 필요팝업안내여부필요발송위신청, 사용자후발송위신청)
  applicationReleaseCheck({
    robotId: lastPublishData.robotId,
    version: lastPublishData.version,
    source: 'publish',
  }, async (params) => {
    // 필요위신청
    const releaseRes = await releaseWithPublish({ robotId: lastPublishData.robotId, robotVersion: lastPublishData.version, name: lastPublishData.name, ...params }) as { data?: string }
    message.success(releaseRes.data || t('publish.autoCheckSuccess'))
  }, () => { // 아니오필요위신청, 기존발송버전
    // 예결과공유경과마켓, data내용로market, 예결과공유경과, 내용로create, 안내
    if (Number(basicFormData.value.version) > 1 && res.data === 'market') {
      message.success(t('publish.syncMarketSuccess'))
    }
  })
  emits('submited')
}
</script>

<template>
  <div class="publish-container w-full h-full flex flex-col overflow-hidden pt-5">
    <div class="publish-content flex-1 overflow-y-auto h-full">
      <div class="basic-form">
        <BasicFormComponent
          ref="basicForm"
          v-model="basicFormData"
          :robot-id="props.robotId"
        />
      </div>
    </div>
    <div class="publish-footer flex h-[60px] items-center justify-between px-6">
      <Checkbox v-if="!isFirstVerison" v-model:checked="enableLastVersion">
        {{ t('publish.syncEnableVersion') }}
      </Checkbox>
      <Button type="primary" @click="handleSubmit">
        {{ t('publish.btn') }}
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.publish-container {
  background: $color-bg-container;
}
.publish-header {
  height: 64px;
  line-height: 64px;
  margin-right: 36px;
}
.publish-content {
  &::webkit-scrollbar {
    display: none;
  }
}
.title {
  font-size: 18px;
  font-weight: 600;
}
</style>