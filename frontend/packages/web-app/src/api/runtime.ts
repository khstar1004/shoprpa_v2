import { storage } from '@/utils/storage'

import { utilsManager } from '@/platform'
import { isWorkflowEditorSmokeMode } from '@/smoke/workflowEditorSmoke'

const DEFAULT_LOCAL_ROUTE_PORT = 13159
const DEFAULT_REMOTE_GATEWAY_PORT = 32742

function readPort(url?: string): number | null {
  if (!url)
    return null

  try {
    return Number(new URL(url).port) || null
  }
  catch {
    return null
  }
}

export async function hasLocalRuntimeRoute(): Promise<boolean> {
  if (isWorkflowEditorSmokeMode())
    return false

  if (utilsManager.isBrowser)
    return false

  const routePort = Number(storage.get('route_port')) || DEFAULT_LOCAL_ROUTE_PORT
  const appConfig = await utilsManager.getAppConfig().catch(() => null)
  const remotePort = readPort(appConfig?.remote_addr) || DEFAULT_REMOTE_GATEWAY_PORT

  return routePort !== remotePort
}
