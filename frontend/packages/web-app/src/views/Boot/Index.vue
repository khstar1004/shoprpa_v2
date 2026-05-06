<script setup lang="ts">
import { Auth } from '@rpa/components/auth'
import { theme } from 'ant-design-vue'
import { to } from 'await-to-js'
import { storeToRefs } from 'pinia'
import { nextTick, onMounted, onUnmounted, ref } from 'vue'

import { base64ToString } from '@/utils/common'
import BUS from '@/utils/eventBus'
import { storage } from '@/utils/storage'

import { expiredModal, getAPIBaseURL } from '@/api/http/env'
import BootHeader from '@/components/Boot/Header.vue'
import LaunchCarousel from '@/components/Boot/LaunchCarousel.vue'
import ConfigProvider from '@/components/ConfigProvider/index.vue'
import Loading from '@/components/Loading.vue'
import heroBg from '@/assets/brand/shoprpa-hero-bg.png'
import { isBrowser, utilsManager, windowManager } from '@/platform'
import { useAppConfigStore } from '@/stores/useAppConfig'

const { token } = theme.useToken()
const appStore = useAppConfigStore()
const { appInfo } = storeToRefs(appStore)
const progress = ref(0)
const isLogin = ref(false)
const loginFormRef = ref()
const autoLogin = ref(true)

function loginWindowStep() {
 windowManager.restoreLoginWindow()
}

function launchProgressCallback(msg: { step: number }) {
 progress.value = msg.step
}

utilsManager.listenEvent('scheduler-event', (eventMsg) => {
 const msgObject = JSON.parse(base64ToString(eventMsg))
 const { type, msg } = msgObject
 console.log('주프로세스메시지: ', msgObject)
 switch (type) {
 case 'sync': {
 // 시작정도
 launchProgressCallback(msg)
 break
 }
 case 'sync_cancel': {
 storage.set('route_port', msg?.route_port)
 sessionStorage.setItem('launch', '1')
 loginAuto()
 break
 }
 default:
 break
 }
})

function loginAuto() {
 if (sessionStorage.getItem('launch') === '1') {
 isLogin.value = true
 const searchParams = new URLSearchParams(window.location.search)
 const code = searchParams.get('code')
 const tenantType = searchParams.get('tenantType')
 autoLogin.value = !code
 if (code === '900005') {
 expiredModal(tenantType)
 nextTick(() => {
 console.log(loginFormRef)
 if (tenantType === 'professional')
 loginFormRef.value.autoPreLogin()
 })
 }
 }
}

function loginSuccess(userInfo: any) {
 console.log('로그인성공: ', userInfo)
 location.replace(`/index.html`)
}

onMounted(async () => {
 loginWindowStep()
 const appConfig = await utilsManager.getAppConfig()
 if (appConfig?.remote_addr) {
 const url = new URL(appConfig.remote_addr)
 storage.set('route_port', Number(url.port) || 32742)
 sessionStorage.setItem('launch', '1')
 loginAuto()
 }
})

window.onload = async () => {
 loginAuto()
 if (isBrowser)
 return

 const [err] = await to(utilsManager.invoke('main_window_onload'))
 if (err) {
 console.error('main_window_onload 호출실패: ', err)
 }
}

onUnmounted(() => {
 BUS.$off('launch-progress', launchProgressCallback)
})
</script>

<template>
 <ConfigProvider>
 <Auth.PageLayout>
 <template #header>
 <BootHeader />
 </template>
 <template v-if="!isLogin" #container>
 <div class="boot-shell" :style="{ backgroundImage: `linear-gradient(90deg, rgba(6,10,16,0.92), rgba(6,10,16,0.52)), url(${heroBg})` }">
 <div class="boot-copy">
 <div class="boot-kicker">shoprpa</div>
 <h1>반복 업무를 실행 가능한 자동화로 전환합니다.</h1>
 <p>업무 흐름 설계, 실행 예약, 기록 관리를 한 화면에서 안정적으로 운영하세요.</p>
 </div>
 <LaunchCarousel>
 <template #footer>
 <div class="mt-6 w-[280px]">
 <a-progress
 :percent="progress"
 :show-info="false"
 :stroke-color="token.colorPrimary"
 trail-color="rgba(255, 255, 255, 0.12)"
 />
 </div>
 </template>
 </LaunchCarousel>
 </div>
 </template>
 <Auth.LoginForm v-if="isLogin" ref="loginFormRef" :base-url="getAPIBaseURL()" :auto-login="autoLogin" :auth-type="appInfo.appAuthType" :edition="appInfo.appEdition" @finish="loginSuccess" />
 </Auth.PageLayout>
 <Loading />
 </ConfigProvider>
</template>

<style lang="scss" scoped>
.boot-shell {
 width: 100vw;
 height: 100vh;
 display: grid;
 grid-template-columns: minmax(320px, 520px) minmax(360px, 1fr);
 align-items: center;
 gap: 56px;
 padding: 72px clamp(32px, 7vw, 112px) 48px;
 background-position: center;
 background-size: cover;
}

.boot-copy {
 color: #fff;
 max-width: 520px;
}

.boot-kicker {
 margin-bottom: 18px;
 color: #2dd4bf;
 font-size: 13px;
 font-weight: 700;
 letter-spacing: 0;
}

.boot-copy h1 {
 margin: 0;
 color: #fff;
 font-size: clamp(34px, 4vw, 56px);
 line-height: 1.08;
 font-weight: 760;
 letter-spacing: 0;
}

.boot-copy p {
 margin-top: 18px;
 color: rgba(255, 255, 255, 0.72);
 font-size: 16px;
 line-height: 1.7;
}

@media (max-width: 900px) {
 .boot-shell {
 grid-template-columns: 1fr;
 justify-items: center;
 padding: 64px 24px 32px;
 }

 .boot-copy {
 text-align: center;
 }
}
</style>
