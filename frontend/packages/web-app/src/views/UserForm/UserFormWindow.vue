<script setup lang="ts">
import { useElementSize } from '@vueuse/core'
import { onMounted, toRaw, useTemplateRef } from 'vue'

import { WINDOW_NAME } from '@/constants'
import { windowManager } from '@/platform'
import type { AnyObj } from '@/types/common'
import UserFormDialog from '@/views/Arrange/components/customDialog/components/userFormDialog.vue'
import type { DialogOption } from '@/views/Arrange/components/customDialog/types'

import { transformData } from './utils'

const userFormRef = useTemplateRef<HTMLDivElement>('userFormRef')
const useFormSize = useElementSize(userFormRef)

async function resizeWindow() {
 const formHeight = useFormSize.height.value
 // 계획사용자 지정대화상자창높음정도, 높음정도동작
 const { offsetWidth: bodyWidth } = document.body
 // 중단계획높음정도여부초과경과완료에서화면높음정도
 const screenWorkArea = await windowManager.getScreenWorkArea()
 const resHeight = Math.min(formHeight, screenWorkArea.height)
 await windowManager.setWindowSize({ width: bodyWidth, height: resHeight })
 await windowManager.showWindow()
}

const targetInfo = new URL(location.href).searchParams
const windowOption = transformData(JSON.parse(targetInfo.get('option'))) as DialogOption
const replyBaseData = JSON.parse(targetInfo.get('reply') || '{}') ?? {}

onMounted(() => resizeWindow())

function handleClose() {
 windowManager.closeWindow()
}

function handleSave(data: AnyObj) {
 windowManager.emitTo({
 from: WINDOW_NAME.USERFORM,
 target: WINDOW_NAME.MAIN,
 type: 'userFormSave',
 data: { ...replyBaseData, data: toRaw(data) },
 })
}
</script>

<template>
 <div ref="userFormRef">
 <UserFormDialog draggable :option="windowOption" @close="handleClose" @save="handleSave" />
 </div>
</template>
