import { Icon, NiceModal } from '@rpa/components'
import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { storeToRefs } from 'pinia'
import { computed, h, ref } from 'vue'

import { canAchieveApp, deleteApp, getPushHistoryVersions, useApplication } from '@/api/market'
import { useAppConfigStore } from '@/stores/useAppConfig'
import { useUserStore } from '@/stores/useUserStore'
import { useCommonOperate } from '@/views/Home/pages/hooks/useCommonOperate.tsx'

import type { cardAppItem } from '../../../types/market'
import { DeployRobotModal, MarketAchieveModal, VersionPushModal } from '../index'

export function useCardsShow(emits) {
 const { t } = useTranslation()
 const consultRef = ref(null)
 const appStore = useAppConfigStore()
 const userStore = useUserStore()
 const { appInfo } = storeToRefs(appStore)

 const { handleDeleteConfirm, useApplicationConfirm } = useCommonOperate()

 function safeErrorMessage(err: any, fallback = t('requestFailedRetry')) {
 const raw = err?.message || err?.msg
 return String(raw || '').toLowerCase() === 'unauthorized'
 ? t('noPermission')
 : raw || fallback
 }

 const appDrawerData = ref({
 visible: false,
 data: null,
 })

 function showAppDrawer(item) {
 console.log(item)
 appDrawerData.value = {
 visible: true,
 data: item,
 }
 }

 function closeAppDrawer(data: { appId: string, checkNum: number }) {
 appDrawerData.value = {
 visible: false,
 data: null,
 }
 emits('updateCheckNum', data)
 }

 async function handleDeleteApp(item) {
 const { appId, marketId } = item
 const confirm = await handleDeleteConfirm(t('market.unpublishConfirm', { name: item.appName }))
 if (!confirm) {
 return
 }
 await deleteApp({ appId, marketId })
 message.success(t('common.operationSuccess'))
 emits('refreshHomeTable')
 }

 async function handleAppAchieve(e: Event, item) {
 e.stopPropagation()
 const { appId, marketId } = item
 let needApplication = false
 let error = false
 try {
 const { data } = await canAchieveApp({ appId, marketId })
 needApplication = data === 0
 }
 catch (e) {
 error = true
 message.error(safeErrorMessage(e))
 }

 if (error)
 return
 // 색상비밀단계및색상비밀단계아니오예가능사용부서의사람원,필요발송사용신청
 if (needApplication) {
 useApplicationConfirm(t('market.insufficientPermissionNeedApplyConfirm'), () => {
 useApplication({ appId, marketId }).then(() => {
 message.success(t('market.applicationSent'))
 }).catch((e) => {
 message.error(safeErrorMessage(e, t('market.applicationSendFail')))
 })
 })
 return
 }

 const data = await getPushHistoryVersions(item)
 NiceModal.show(MarketAchieveModal, {
 record: item,
 versionLst: data,
 onRefresh: () => emits('refreshHomeTable'),
 onLimit: () => {
 consultRef.value?.init({
 authType: appInfo.value.appAuthType,
 trigger: 'modal',
 modalConfirm: {
 title: t('designerManage.limitReachedTitle'),
 content: userStore.currentTenant?.tenantType === 'personal'
 ? t('designerManage.personalLimitReachedContent')
 : t('designerManage.proLimitReachedContent'),
 okText: userStore.currentTenant?.tenantType === 'personal'
 ? t('designerManage.upgradeToPro')
 : t('designerManage.upgradeToEnterprise'),
 cancelText: t('designerManage.gotIt'),
 },
 consult: {
 consultTitle: t('designerManage.consult'),
 consultEdition: userStore.currentTenant?.tenantType === 'personal' ? 'professional' : 'enterprise',
 consultType: 'consult',
 },
 })
 },
 })
 }

 const menus = computed(() => [
 {
 key: 'deploy',
 label: t('common.deploy'),
 icon: h(Icon, { name: 'deploy' }),
 },
 {
 key: 'push',
 label: t('market.versionPush'),
 icon: h(Icon, { name: 'tools-publish' }),
 },
 {
 key: 'delete',
 label: t('unpublish'),
 icon: h(Icon, { name: 'market-del' }),
 },
 ],
 )

 function handleAppDeploy(cardItem: cardAppItem) {
 NiceModal.show(DeployRobotModal, { record: cardItem })
 }

 function handleAppPush(cardItem: cardAppItem) {
 NiceModal.show(VersionPushModal, { record: cardItem })
 }

 function menuItemClick(key, cardItem) {
 switch (key) {
 case 'deploy':
 handleAppDeploy(cardItem)
 break
 case 'push':
 handleAppPush(cardItem)
 break
 case 'delete':
 handleDeleteApp(cardItem)
 break
 default:
 break
 }
 }
 return {
 consultRef,
 appInfo,
 appDrawerData,
 showAppDrawer,
 closeAppDrawer,
 handleAppAchieve,
 menus,
 menuItemClick,
 }
}
