<script setup lang="ts">
import { useAsyncState, watchDeep } from '@vueuse/core'
import { DatePicker, Form, Select, Spin } from 'ant-design-vue'
import dayjs from 'dayjs'
import { reactive } from 'vue'

import { getRobotRecordOverview } from '@/api/robot'
import NDataCards from '@/components/NDataCards/index.vue'
import type { DataCardItem } from '@/components/NDataCards/index.vue'
import RecordTable from '@/views/Home/components/RecordTable/index.vue'

import { useBasicStore } from './basicStore'

const props = defineProps<{ robotId: string, version: number }>()

const { data: basicInfo } = useBasicStore()

const formState = reactive({
  deadline: dayjs(),
  version: props.version,
})

const { state, isLoading, execute } = useAsyncState<DataCardItem[]>(
  getRobotRecordOverview({
    robotId: props.robotId,
    version: formState.version,
    deadline: formState.deadline.format('YYYY-MM-DD'),
  }),
  [],
)

function disabledDate(current: dayjs.Dayjs) {
  return current && dayjs().isBefore(current)
}

watchDeep(formState, () => execute())
</script>

<template>
  <Spin :spinning="isLoading">
    <div class="text-base font-semibold mb-[12px]">
      실행
    </div>
    <Form
      layout="vertical"
      :colon="false"
      class="flex gap-4"
    >
      <Form.Item label="중지날짜" class="flex-1 mb-3">
        <DatePicker
          v-model:value="formState.deadline"
          :disabled-date="disabledDate"
          :allow-clear="false"
          class="w-full"
        />
      </Form.Item>
      <Form.Item label="선택버전" class="flex-1 mb-3">
        <Select v-model:value="formState.version">
          <Select.Option v-for="item in basicInfo.versionInfoList" :key="item.versionNum" :value="item.versionNum">
            {{ `버전${item.versionNum}` }}
          </Select.Option>
        </Select>
      </Form.Item>
    </Form>
    <NDataCards :list="state" />
    <div class="text-base font-semibold mt-[20px] mb-[6px]">
      실행기록
    </div>
    <RecordTable class="!h-[340px]" :robot-id="props.robotId" />
  </Spin>
</template>