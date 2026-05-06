import type { IPluginContext } from '@rpa/shared'
import { definePlugin } from '@rpa/shared'
import { markRaw } from 'vue'

import Content from './Content.vue'

export default definePlugin({
  activate: async (_context: IPluginContext) => {
    console.log('[SimplePluginExample] 확장중...')
  },
  deactivate: () => {
    console.log('[SimplePluginExample] 확장중지사용중...')
    console.log('[SimplePluginExample] 확장중지사용완료')
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