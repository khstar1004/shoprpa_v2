import { VxeLoading, VxeTooltip, VxeUI } from 'vxe-pc-ui'
import type { VxeGridProps, VxeTablePropTypes } from 'vxe-table'
import { VxeGrid } from 'vxe-table'

import 'vxe-table/lib/style.css'
import 'vxe-pc-ui/lib/style.css'
import koKR from 'vxe-table/lib/locale/lang/ko-KR'

VxeUI.setI18n('zh-CN', koKR)
VxeUI.setLanguage('zh-CN')
VxeUI.component(VxeGrid)
VxeUI.component(VxeTooltip)
VxeUI.component(VxeLoading)

export type { VxeGridProps, VxeTablePropTypes }
export default VxeGrid
