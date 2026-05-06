<script lang="ts" setup>
import { useRunlogStore } from '@/stores/useRunlogStore'

function clearLog() {
 useRunlogStore().clearLogs()
}
</script>

<template>
 <rpa-hint-icon
 name="clear-outlined"
 title="지우기로그"
 enable-hover-bg
 @click="() => clearLog()"
 />
</template>
