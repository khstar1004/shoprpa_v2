import { createPinia } from 'pinia'
import { createApp } from 'vue'

import i18next from '@/plugins/i18next'

// 가져오기기본값파일관리
import '@/utils/event'
import '@/assets/css/default.css'

import Index from './Index.vue'

const app = createApp(Index)

app.use(createPinia())
app.use(i18next)
app.mount('#app')
