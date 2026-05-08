import type { IPluginContext } from '@rpa/shared'
import { definePlugin } from '@rpa/shared'
import { markRaw } from 'vue'

import Content from './Content.vue'

export default definePlugin({
  activate: async (_context: IPluginContext) => {
    console.log('[SimplePluginExample] 확장이 시작되었습니다.')
  },
  deactivate: () => {
    console.log('[SimplePluginExample] 확장을 중지하는 중입니다.')
    console.log('[SimplePluginExample] 확장이 중지되었습니다.')
  },
  contributes: {
    settingsTabs: [
      {
        id: 'text',
        title: '시도',
        icon: 'plugin',
        content: markRaw(Content),
      },
    ],
  },
})
