import type { SegmentedProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import {
  acceptJoinTeam,
  getNewMessage,
  messageList,
  refuseJoinTeam,
  setAllRead,
  setMessageReadById,
} from '@/api/market'
import { APPLICATIONMARKET, EDITORPAGE, TEAMMARKETS } from '@/constants/menu'
import { useRoutePush } from '@/hooks/useCommonRoute'
import { useMarketStore } from '@/stores/useMarketStore'
import { useProcessStore } from '@/stores/useProcessStore'

import { ALLREADNUM, JOINNUM, NOREADNUM, READNUM, REFUSENUM, TEAMMARKETUPDATE } from '../config'

export function useMessageTip() {
  const processStore = useProcessStore()
  const { t } = useTranslation()
  const route = useRoute()
  let loopTimer = null
  const showMessage = ref(false)
  const messageData = ref([])
  const messageBoxRef = ref(null)
  const page = ref({
    pageNo: 1,
    pageSize: 10,
    total: 0,
    totalPages: 1,
  })
  const hasBadage = ref('0')
  const loading = ref(false) // 사용메시지데이터, 중지일직선, 다중트리거요청
  const spinStatus = ref(false) // 사용열기메시지목록시, spin
  const readType = ref('all')

  const tabs = computed<SegmentedProps['options']>(() => ([
    {
      label: t('settingCenter.message.unreadMessage'),
      value: 'noread',
    },
    {
      label: t('settingCenter.message.allMessage'),
      value: 'all',
    },
  ]))

  function extractBracketContent(messageInfo: string): string {
    if (!messageInfo || typeof messageInfo !== 'string') {
      return ''
    }
    // 사용정상이면테이블방식매칭방법내용
    const regex = /\[([^\]]+)\]/g
    const content = messageInfo.replace(regex, match => `<span class="font-semibold">${match}</span>`)

    return content
  }
  function handleTabChange(value: string) {
    if (value === 'noread') {
      checkNoread()
    }
    else {
      checkAll()
    }
  }

  const getMessageList = () => {
    if (page.value.pageNo > page.value.totalPages)
      return
    const { pageNo, pageSize } = page.value
    // 가져오기메시지목록
    messageList({ pageNo, pageSize }).then((data) => {
      if (data) {
        const { records, pages, total } = data
        page.value.totalPages = pages
        page.value.total = total
        messageData.value = [...messageData.value, ...records]
      }
    }).finally(() => {
      spinStatus.value = false
      loading.value = false
    })
  }

  const scroll = () => {
    const { clientHeight, scrollHeight, scrollTop } = messageBoxRef.value
    if (clientHeight + scrollTop + 60 >= scrollHeight && !loading.value) {
      loading.value = true
      page.value.pageNo++
      getMessageList()
    }
  }

  // 새로고침메시지
  const refresh = async () => {
    hasBadage.value = await getNewMessage()
  }

  const toastMessage = (data, custom, id?) => {
    if (data)
      message.success(t('common.operationSuccess'))
    messageData.value = messageData.value.map((item) => {
      if (item.id === id)
        item.operateResult = custom
      if (custom === ALLREADNUM && item.operateResult === NOREADNUM)
        item.operateResult = READNUM
      return item
    })
  }

  const toMarket = (id: string) => {
    useMarketStore().refreshTeamList(id)
    useRoutePush({ name: TEAMMARKETS, query: { marketId: id } })
  }

  // 클릭메시지
  const readMessage = async ({ operateResult, id, messageType, marketId }) => {
    if (messageType === TEAMMARKETUPDATE) {
      // 정렬페이지, 클릭메시지, 저장정렬페이지데이터, 변환까지마켓사용
      if (route.name === EDITORPAGE) {
        await processStore.saveProject()
      }
      toMarket(marketId)
      showMessage.value = false
    }
    if (operateResult !== NOREADNUM)
      return
    const data = await setMessageReadById({ notifyId: id })
    toastMessage(data, READNUM, id)
    refresh()
  }

  // 조회미완료
  const checkNoread = () => {
    messageData.value = messageData.value.filter(i => i.operateResult === NOREADNUM)
  }

  // 전체메시지
  const checkAll = () => {
    messageData.value = []
    getMessageList()
  }

  // 전체완료
  const allRead = async () => {
    const data = await setAllRead()
    toastMessage(data, ALLREADNUM)
    refresh()
  }

  // 추가입력팀
  const joinTeam = async (id) => {
    const data = await acceptJoinTeam({ notifyId: id })
    toastMessage(data, JOINNUM, id)
    if (route.meta.resource === APPLICATIONMARKET) {
      useMarketStore().refreshTeamList()
    }
  }

  // 추가입력팀
  const refuseTeam = async (id) => {
    const data = await refuseJoinTeam({ notifyId: id })
    toastMessage(data, REFUSENUM, id)
  }

  // 열기팝업, 가져오기메시지
  watch(showMessage, (val) => {
    if (val) {
      spinStatus.value = true
      getMessageList()
    }
    else {
      page.value.pageNo = 1
      page.value.totalPages = 1
      messageData.value = []
      spinStatus.value = false
    }
  }, { immediate: true })

  onMounted(() => {
    // 열기시작문의 여부있음새메시지
    loopTimer && clearInterval(loopTimer)
    loopTimer = setInterval(refresh, 20000)
  })

  onBeforeUnmount(() => {
    loopTimer && clearInterval(loopTimer)
  })

  return {
    hasBadage,
    showMessage,
    spinStatus,
    readType,
    tabs,
    handleTabChange,
    messageData,
    messageBoxRef,
    page,
    scroll,
    readMessage,
    checkNoread,
    checkAll,
    allRead,
    joinTeam,
    refuseTeam,
    extractBracketContent,
  }
}
