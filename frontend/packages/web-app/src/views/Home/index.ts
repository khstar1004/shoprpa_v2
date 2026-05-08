import { createPinia } from 'pinia'
import { createApp } from 'vue'

// 방식가져오기
import '@/assets/css/default.css'
import '@/assets/css/main.scss'

import i18next from '@/plugins/i18next'
import sentry from '@/plugins/sentry'

// 도구가져오기
import '@/utils/event'

// 경로에서가져오기
import router from '@/router/index'
import { installWorkflowEditorSmokeHarness } from '@/smoke/workflowEditorSmoke'

// 컴포넌트가져오기
import App from './App.vue'

const app = createApp(App)

app.use(createPinia())
app.use(sentry)
app.use(i18next)
app.use(router)

app.mount('#app')

router.isReady().then(() => {
  installWorkflowEditorSmokeHarness()
})
