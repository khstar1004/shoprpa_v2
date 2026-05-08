import { useMediaQuery } from '@vueuse/core'
import { Modal } from 'ant-design-vue'
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { acceptEnterpriseInvite, acceptMarketInvite, queryEnterpriseInviteData, queryMarketInviteData } from '../../../api/invite'
import { loginStatus, userInfo } from '../../../api/login'
import type { InviteInfo } from '../../../interface'

type PageStatus = 'init' | 'linkExpired' | 'needLogin' | 'showUserInfo' | 'joinSuccess' | 'joined' | 'reachLimited' | 'marketFull'

export function useInviteFlow(emits: { (e: 'joinSuccess'): void }) {
  const isMobile = useMediaQuery('(max-width: 768px)')
  const route = useRoute()
  const inviteKey = ref(route.query.inviteKey as string)
  const currentStatus = ref<PageStatus>()
  const inviteInfo = ref<InviteInfo>({
    resultCode: '',
    inviteType: (route.query.inviteType || 'market') as 'market' | 'enterprise',
    deptName: '',
    marketName: '',
    inviterName: '',
  })
  const currentUser = ref<{ userName: string, phone: string }>({ userName: '', phone: '' })

  const switchPage = (status: PageStatus) => {
    currentStatus.value = status
  }

  const updateInviteInfo = async (data: InviteInfo, needLogin = true) => {
    inviteInfo.value = { ...inviteInfo.value, ...data }
    let pageStatus: PageStatus = 'needLogin'
    switch (data.resultCode) {
      case '101':
        pageStatus = 'reachLimited'
        break
      case '102':
        pageStatus = 'linkExpired'
        break
      case '100':
        pageStatus = 'marketFull'
        break
      case '001':
        pageStatus = 'joined'
        break
      case '000':
        pageStatus = 'joinSuccess'
        emits('joinSuccess')
        break
      default:
        break
    }
    if (pageStatus !== 'needLogin') {
      switchPage(pageStatus)
      return
    }
    if (needLogin)
      login()
  }

  const login = async () => {
    const isLogin = await loginStatus()
    if (!isLogin) {
      switchPage('needLogin')
      return
    }
    const user = await userInfo()
    currentUser.value = user
    switchPage('showUserInfo')
  }

  const getInviteInfo = async () => {
    if (!inviteKey.value) {
      switchPage('linkExpired')
      return
    }
    try {
      const func = inviteInfo.value.inviteType === 'market' ? queryMarketInviteData : queryEnterpriseInviteData
      const data = await func({ inviteKey: inviteKey.value })
      const needLogin = !isMobile.value
      updateInviteInfo(data, needLogin)
      if (isMobile.value) {
        switchPage('init')
      }
    }
    catch (e) {
      console.error('초대 정보를 가져오지 못했습니다.', e)
      switchPage('linkExpired')
    }
  }

  const toJoin = async () => {
    try {
      const func = inviteInfo.value.inviteType === 'market' ? acceptMarketInvite : acceptEnterpriseInvite
      const data = await func({ inviteKey: inviteKey.value })
      updateInviteInfo(data, false)
    }
    catch (e) {
      console.error('초대 참여에 실패했습니다.', e)
      Modal.error({
        title: '초대 참여 실패',
        content: '초대 참여 처리 중 오류가 발생했습니다. 잠시 후 다시 시도하세요.',
        okText: '확인',
      })
    }
  }

  const tryOpenApp = (scheme: string, timeout = 2000) => {
    const start = Date.now()
    const clear: () => void = () => {
      clearTimeout(timer)
      window.removeEventListener('blur', clear)
    }
    window.addEventListener('blur', clear)

    window.location.href = scheme

    const timer = setTimeout(() => {
      if (Date.now() - start >= timeout + 100)
        return

      Modal.warn({
        title: '설치안내',
        content: 'ShopRPA가 설치되어 있지 않습니다. 관리자에게 설치 패키지를 요청하세요.',
        okText: '확인',
      })
    }, timeout)
  }

  const openApp = () => {
    tryOpenApp('shoprpa://')
  }

  watch(() => route.query, (val) => {
    if (val.inviteKey && val.inviteKey !== inviteKey.value) {
      inviteKey.value = val.inviteKey as string
      inviteInfo.value = {
        ...inviteInfo.value,
        inviteType: (val.inviteType || 'market') as 'market' | 'enterprise',
      }
      getInviteInfo()
    }
  })

  getInviteInfo()

  return {
    currentStatus,
    inviteInfo,
    currentUser,
    switchPage,
    login,
    toJoin,
    openApp,
  }
}
