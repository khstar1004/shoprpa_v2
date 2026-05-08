<script setup lang="ts">
import { TabPane, Tabs } from 'ant-design-vue'
import { computed, ref, watch } from 'vue'

import type { AsyncAction, AuthType, Edition, InviteInfo, LoginMode } from '../../interface'
import DynamicForm from '../Base/DynamicForm.vue'
import FormLayout from '../Base/FormLayout.vue'

import { useLoginForm } from './hooks/useLoginForm'

const { running, errorMessage, inviteInfo, edition, authType } = defineProps({
  running: {
    type: String as () => AsyncAction,
    default: 'IDLE',
  },
  errorMessage: {
    type: String,
    default: '',
  },
  inviteInfo: {
    type: Object as () => InviteInfo,
    default: () => null,
  },
  edition: {
    type: String as () => Edition,
    default: 'saas',
  },
  authType: {
    type: String as () => AuthType,
    default: 'uap',
  },
})

const emit = defineEmits<{
  submit: [data: any, mode: LoginMode]
  switchToRegister: []
  forgotPassword: []
  modifyPassword: []
}>()

const sharedAgreement = ref(false)

const passwordLoading = computed(() => running === 'PASSWORD')
const codeLoading = computed(() => running === 'CODE')

const account = useLoginForm('PASSWORD', { inviteInfo, edition, authType }, emit as any)
const phone = useLoginForm('CODE', { inviteInfo, edition, authType }, emit as any)

const currentMode = ref<LoginMode>('PASSWORD')

watch(() => currentMode.value, (_, old) => (old === 'PASSWORD' ? account : phone).clearValidates())

account.formData.agreement = sharedAgreement.value
phone.formData.agreement = sharedAgreement.value

watch(() => account.formData.agreement, (v) => {
  sharedAgreement.value = v || false
  phone.formData.agreement = v || false
})

watch(() => phone.formData.agreement, (v) => {
  sharedAgreement.value = v || false
  account.formData.agreement = v || false
})
</script>

<template>
  <FormLayout
    wrap-class="auth-login h-full"
    :invite-info="inviteInfo"
  >
    <template v-if="!inviteInfo" #header>
      <div class="auth-login-title text-[#000000D9] mb-[8px] font-[600] text-center dark:text-[#FFFFFF] font-sans">
        {{ $t('auth.welcome', { app: $t('app') }) }}
      </div>
      <div class="text-[12px] text-[#000000A6] mb-[24px] text-center dark:text-[#FFFFFF] font-sans">
        {{ $t('auth.useAuthAccount', { auth: $t('auth.shoprpa') }) }}
      </div>
      <div
        v-if="errorMessage"
        data-shoprpa-auth-error-state
        role="alert"
        class="auth-error-state"
      >
        <div class="auth-error-state__title">
          서비스 연결을 확인하세요
        </div>
        <div class="auth-error-state__message">
          {{ errorMessage }}
        </div>
      </div>
    </template>
    <Tabs
      v-model:active-key="currentMode"
      centered
      :class="{ 'tab-pane-text-left': !phone.config || !account.config }"
      type="card"
      class="h-full"
    >
      <TabPane v-if="account.config" key="PASSWORD" :tab="$t('auth.passwordLogin')">
        <DynamicForm
          :ref="account.formRef"
          v-model="account.formData"
          :loading="passwordLoading"
          :config="account.config"
          :handle-events="account.handleEvents"
        />
      </TabPane>

      <TabPane v-if="phone.config" key="CODE" :tab="$t('auth.codeLogin')">
        <DynamicForm
          :ref="phone.formRef"
          v-model="phone.formData"
          :loading="codeLoading"
          :config="phone.config"
          :handle-events="phone.handleEvents"
        />
      </TabPane>
    </Tabs>
  </FormLayout>
</template>

<style scoped>
.auth-login-title {
  font-size: 23px;
  line-height: 1.35;
  word-break: keep-all;
  overflow-wrap: normal;
}

.auth-error-state {
  margin: -8px 0 16px;
  padding: 10px 12px;
  border: 1px solid #ffd8bf;
  border-radius: 8px;
  background: #fff7f0;
  color: #7a2e0e;
  font-family: inherit;
}

.auth-error-state__title {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
}

.auth-error-state__message {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.45;
  word-break: keep-all;
  overflow-wrap: anywhere;
}
</style>
