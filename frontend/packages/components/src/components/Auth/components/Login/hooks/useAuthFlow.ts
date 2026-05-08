import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { ref, watch } from 'vue'

import { setBaseUrl } from '../../../api/http'
import {
  isHistory,
  login,
  loginStatus,
  modifyPassword,
  preAuthenticate,
  register,
  setPassword,
  submitConsult,
  tenantList,
} from '../../../api/login'
import type {
  AsyncAction,
  AuthFormMode,
  AuthType,
  ConsultFormData,
  Edition,
  InviteInfo,
  LoginFormData,
  LoginMode,
  Platform,
  RegisterFormData,
  RegisterMode,
  TenantItem,
} from '../../../interface'
import { clearRememberUser, getRememberUser, getSelectedTenant, saveRememberUser, saveSelectedTenant } from '../../../utils/remember'

export interface UseAuthFlowOptions {
  baseUrl?: string
  platform?: Platform
  inviteInfo?: InviteInfo
  authType?: AuthType
  edition?: Edition
  autoLogin?: boolean
}

export function useAuthFlow(opts: UseAuthFlowOptions = {}, emits: { (e: 'finish'): void }) {
  const { t } = useTranslation()
  const platform = ref<Platform>(opts.platform || 'admin')
  const preFormMode = ref<AuthFormMode>('login')
  const currentFormMode = ref<AuthFormMode>('login')
  const tenants = ref<TenantItem[]>([])
  const tempToken = ref<string>('')
  const running = ref<AsyncAction>('IDLE')
  const cacheFormData = ref<Record<string, any>>({})
  const lastError = ref('')

  const getErrorMessage = (error: any, fallback: string) => {
    const raw = error?.userMessage || error?.message || error?.msg || error?.data?.message || error?.data?.msg
    return String(raw || fallback || '').trim()
  }

  const setActionError = (error: any, fallback: string) => {
    lastError.value = getErrorMessage(error, fallback)
  }

  const clearActionError = () => {
    lastError.value = ''
  }

  const setCacheFormData = (data: any) => {
    const cacheKey = currentFormMode.value
    cacheFormData.value[cacheKey] = data
  }

  const resetCacheFormData = () => {
    cacheFormData.value = {}
  }

  const run = async <T>(action: AsyncAction, task: () => Promise<T>) => {
    running.value = action
    clearActionError()
    try {
      return await task()
    }
    finally {
      running.value = 'IDLE'
    }
  }

  watch(
    () => opts.baseUrl,
    (newVal) => {
      newVal && setBaseUrl(newVal)
      if (!opts.platform) {
        platform.value = newVal && (newVal.includes('localhost') || newVal.includes('127.0.0.1')) ? 'client' : 'admin'
      }
    },
    { immediate: true },
  )

  const checkLoginStatus = async () => {
    try {
      const isLogin = await loginStatus()
      if (isLogin) {
        checkTenant(getSelectedTenant())
      }
    }
    catch (e) {
      setActionError(e, '백엔드 연결 상태를 확인할 수 없습니다. 서버 주소와 서비스 실행 상태를 확인해 주세요.')
      currentFormMode.value = 'login'
    }
  }

  const checkTenant = async (tenantId: string | null) => {
    try {
      const data = await tenantList()
      tenants.value = data
      if (tenantId) {
        const tenantExists = tenants.value.find(t => t.id === tenantId)
        if (tenantExists && !tenantExists.isExpired) {
          emits('finish')
          return
        }
      }
      switchMode('login')
    }
    catch (e) {
      setActionError(e, t('components.auth.fetchTenantsFailed'))
      console.error(t('components.auth.fetchTenantsFailed'), e)
    }
  }

  const switchToTenants = async (autoLogin = true) => {
    try {
      const data = await tenantList(tempToken.value)
      tenants.value = data

      if (autoLogin && tenants.value.length === 1 && tenants.value[0]?.id) {
        handleLogin(tenants.value[0].id)
        return
      }

      if (tenants.value.length === 0) {
        message.info(t('components.auth.noAvailableSpace'))
        return
      }

      switchMode('tenantSelect')
    }
    catch (e) {
      setActionError(e, t('components.auth.fetchTenantsFailed'))
      console.error(t('components.auth.fetchTenantsFailed'), e)
    }
  }

  const preLogin = async (data: LoginFormData, mode: LoginMode, autoLogin = true) => run(mode, async () => {
    try {
      const params = { ...data, loginType: mode }
      if (opts.edition === 'saas' && opts.authType === 'uap') {
        const history = await isHistory({ phone: params.phone })
        if (history) {
          if (mode === 'PASSWORD')
            switchMode('forgotPasswordWithSysUpgrade')
          if (mode === 'CODE') {
            await handleForgotPassword(params, 'login')
          }
          return
        }
      }
      const account = params.phone || params.loginName
      mode === 'PASSWORD' && params.remember && account && params.password ? saveRememberUser(account, params.password, opts.edition, opts.authType) : clearRememberUser()
      delete params.remember
      delete params.agreement
      const token = await preAuthenticate({ ...params, scene: 'login', platform: platform.value })
      tempToken.value = token
      switchToTenants(autoLogin)
    }
    catch (e) {
      setActionError(e, t('components.auth.loginFailed'))
      console.error(t('components.auth.loginFailed'), e)
    }
  })

  const handleLogin = async (tenantId: string) => {
    try {
      await login({ tenantId, tempToken: tempToken.value })
      saveSelectedTenant(tenantId)
      emits('finish')
    }
    catch (e) {
      setActionError(e, t('components.auth.enterSpaceFailed'))
      console.error(t('components.auth.enterSpaceFailed'), e)
    }
  }

  const handleRegister = async (data: RegisterFormData | ConsultFormData, mode: RegisterMode) => run(mode, async () => {
    try {
      if (mode === 'REGISTER') {
        const token = await register(data as RegisterFormData)
        message.success(t('components.auth.registerSuccess'))
        tempToken.value = token
        if (!Object.prototype.hasOwnProperty.call(data, 'password') || !(data as RegisterFormData).password)
          switchMode('setPassword')
        else switchToTenants()
        return
      }
      await submitConsult(data as ConsultFormData)
      message.success(t('components.auth.submitSuccess'))
      switchMode('login')
    }
    catch {
    }
  })

  const handleForgotPassword = async (data: LoginFormData, scene?: string) => run('FORGOT_PASSWORD', async () => {
    try {
      setCacheFormData(data)
      const params: LoginFormData = { ...data, loginType: 'CODE' }
      delete params.remember
      delete params.agreement
      const token = await preAuthenticate({ ...params, scene: (scene || 'set_password'), platform: platform.value })
      tempToken.value = token
      switchMode(currentFormMode.value === 'forgotPassword' ? 'setPassword' : 'setPasswordWithSysUpgrade')
    }
    catch {
    }
  })

  const handleSetPassword = async (data: LoginFormData) => run('SET_PASSWORD', async () => {
    await setPassword({ ...data, tempToken: tempToken.value })
    message.success('비밀번호완료')
    switchToTenants()
  })

  const handleModifyPassword = async (data: LoginFormData) => run('MODIFY_PASSWORD', async () => {
    const token = await modifyPassword(data)
    message.success('비밀번호가 변경되었습니다.')
    tempToken.value = token
    switchToTenants()
  })

  const handleChooseTenant = async (tenantId: string) => {
    await handleLogin(tenantId)
  }

  const switchMode = (mode: AuthFormMode) => {
    preFormMode.value = currentFormMode.value
    currentFormMode.value = mode
    if (mode === 'login')
      resetCacheFormData()
  }

  const autoPreLogin = () => {
    const remembered = getRememberUser()
    if (remembered) {
      const accountKey = opts.authType === 'uap' ? 'phone' : 'loginName'
      const params = {
        [accountKey]: remembered.account,
        password: remembered.password,
        remember: true,
      }

      preLogin(params, 'PASSWORD', false)
    }
  }

  if (!opts.inviteInfo && (opts.autoLogin !== false)) {
    checkLoginStatus()
  }

  return {
    currentFormMode,
    cacheFormData,
    lastError,
    preFormMode,
    tenants,
    running,
    preLogin,
    handleRegister,
    handleForgotPassword,
    handleSetPassword,
    handleModifyPassword,
    handleChooseTenant,
    switchMode,
    autoPreLogin,
  }
}
