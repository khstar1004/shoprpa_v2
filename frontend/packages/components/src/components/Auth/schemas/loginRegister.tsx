import { Button, Checkbox } from 'ant-design-vue'
import i18next from 'i18next'

import { sendCaptcha } from '../api/login'

import { fieldFactories } from './factories'
import type { FormConfig } from './factories'

/**
 * 로그인회원가입닫기테이블단일매칭
 */

// 계정로그인테이블단일매칭
export function accountLoginFormConfig(isInvite = false, edition = 'saas', authType = 'uap'): FormConfig | null {
  const type = `${edition}_${authType}`
  let conf: FormConfig | null = null
  switch (type) {
    case 'saas_uap': // uap계정로그인(휴대폰 번호), 지원회원가입, 비밀번호(통신경과휴대폰 번호검증인증코드돌아가기비밀번호)
      conf = {
        fields: [
          { ...fieldFactories.phone(), placeholder: i18next.t('authForm.accountPlaceholder') },
          fieldFactories.password(true),
          fieldFactories.agreement(),
          { ...fieldFactories.remember(), hidden: () => isInvite },
        ],
        actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
          <div class="w-full absolute bottom-0">
            <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
              { loading ? i18next.t('authForm.loggingIn') : (isInvite ? i18next.t('authForm.loginAndJoin') : i18next.t('authForm.login')) }
            </Button>
            <div class="text-center text-[14px] mt-[12px] text-[#000000D9] dark:text-[#FFFFFFD9]">
              {i18next.t('authForm.noAccount')}
              <Button type="link" class="!m-0 !p-0 !border-0 h-auto" onClick={() => handleEvents && handleEvents('switchToRegister')}>
                {i18next.t('authForm.register')}
              </Button>
            </div>
          </div>
        ),
      }
      break
    case 'enterprise_uap': // 버전없음회원가입, 비밀번호공가능, 지원수정비밀번호
      conf = {
        fields: [
          { ...fieldFactories.phone(), placeholder: i18next.t('authForm.accountPlaceholder') },
          fieldFactories.password(true),
          fieldFactories.agreement(),
          {
            ...fieldFactories.remember(),
            customRender: (ctx?: { formData?: any, handleEvents?: any }) => {
              const { formData = {}, handleEvents } = ctx ?? {}
              return (
                <div class="w-full flex justify-between items-center">
                  <Checkbox v-model:checked={formData.remember} class="text-[#000000D9] dark:text-[#FFFFFFD9]">
                    {i18next.t('authForm.rememberPassword')}
                  </Checkbox>
                  <Button type="link" class="!m-0 !p-0 !border-0 h-auto" onClick={() => handleEvents && handleEvents('modifyPassword')}>
                    {i18next.t('authForm.modifyPassword')}
                  </Button>
                </div>
              )
            },
          },
        ],
        actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
          <div class="w-full absolute bottom-0">
            <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
              { loading ? i18next.t('authForm.loggingIn') : (isInvite ? i18next.t('authForm.loginAndJoin') : i18next.t('authForm.login')) }
            </Button>
          </div>
        ),
      }
      break
    case 'saas_casdoor': // casdoor계정로그인(휴대폰 번호), 지원하지 않음비밀번호
      conf = {
        fields: [
          { ...fieldFactories.account(), key: 'loginName' },
          fieldFactories.password(true),
          fieldFactories.agreement(),
          {
            ...fieldFactories.remember(),
            customRender: (ctx?: { formData?: any, handleEvents?: any }) => {
              const { formData = {} } = ctx ?? {}
              return (
                <div class="w-full flex justify-between items-center">
                  <Checkbox v-model:checked={formData.remember} class="text-[#000000D9] dark:text-[#FFFFFFD9]">
                    {i18next.t('authForm.rememberPassword')}
                  </Checkbox>
                </div>
              )
            },
          },
        ],
        actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
          <div class="w-full absolute bottom-0">
            <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
              { loading ? i18next.t('authForm.loggingIn') : (isInvite ? i18next.t('authForm.loginAndJoin') : i18next.t('authForm.login')) }
            </Button>
            <div class="text-center text-[14px] mt-[12px] text-[#000000D9] dark:text-[#FFFFFFD9]">
              {i18next.t('authForm.noCasdoorAccount')}
              <Button type="link" class="!m-0 !p-0 h-auto" onClick={() => handleEvents && handleEvents('switchToRegister')}>
                {i18next.t('authForm.register')}
              </Button>
            </div>
          </div>
        ),
      }
      break
    case 'enterprise_casdoor': // casdoor없음버전
      break
    default:
      break
  }
  return conf
}

// 기기로그인테이블단일매칭
export function phoneLoginFormConfig(isInvite = false, edition = 'saas', authType = 'uap'): FormConfig | null {
  const type = `${edition}_${authType}`
  if (type !== 'saas_uap')
    return null
  // saas버전uap인증시스템지원휴대폰 번호검증인증코드로그인
  return {
    fields: [
      fieldFactories.phone(),
      {
        ...fieldFactories.captcha(),
        sendCaptcha: async (phone: string) => {
          await sendCaptcha(phone, 'login', false)
        },
      },
      fieldFactories.agreement(),
    ],
    actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
      <div class="w-full absolute bottom-0">
        <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
          { loading ? i18next.t('authForm.loggingIn') : (isInvite ? i18next.t('authForm.loginAndJoin') : i18next.t('authForm.login')) }
        </Button>
        <div class="text-center text-[14px] mt-[12px] text-[#000000D9] dark:text-[#FFFFFFD9]">
          {i18next.t('authForm.noAccount')}
          <Button type="link" class="!m-0 !p-0 !border-0 h-auto" onClick={() => handleEvents && handleEvents('switchToRegister')}>
            {i18next.t('authForm.register')}
          </Button>
        </div>
      </div>
    ),
  }
}

// 개사람회원가입테이블단일매칭
export function personalRegisterFormConfig(formData: any, isInvite = false, edition = 'saas', authType = 'uap'): FormConfig | null {
  const type = `${edition}_${authType}`
  let conf: FormConfig | null = null
  switch (type) {
    case 'saas_uap': // uap계정회원가입(휴대폰 번호)
      conf = {
        layout: 'vertical',
        fields: [
          fieldFactories.loginName(),
          fieldFactories.phone(),
          {
            ...fieldFactories.captcha(),
            sendCaptcha: async (phone: string) => {
              await sendCaptcha(phone, 'register', true)
            },
          },
          fieldFactories.agreement(),
        ],
        actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
          <div class="w-full absolute bottom-0">
            <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
              { loading ? i18next.t('authForm.registering') : (isInvite ? i18next.t('authForm.registerAndJoin') : i18next.t('authForm.register')) }
            </Button>
            <div class="text-center text-[14px] mt-[12px] text-[#000000D9] dark:text-[#FFFFFFD9]">
              {i18next.t('authForm.hasAccount')}
              <Button type="link" class="!m-0 !p-0 !border-0 h-auto" onClick={() => handleEvents && handleEvents('switchToLogin')}>
                {i18next.t('authForm.loginNow')}
              </Button>
            </div>
          </div>
        ),
      }
      break
    case 'enterprise_uap': // 버전없음회원가입공가능
      break
    case 'saas_casdoor': // casdoor계정회원가입(휴대폰 번호)
      conf = {
        layout: 'vertical',
        fields: [
          { ...fieldFactories.account(), key: 'loginName' },
          fieldFactories.phone(),
          fieldFactories.password(),
          fieldFactories.confirmPassword(formData),
          fieldFactories.agreement(),
        ],
        actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
          <div class="w-full absolute bottom-0">
            <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
              { loading ? i18next.t('authForm.registering') : (isInvite ? i18next.t('authForm.registerAndJoin') : i18next.t('authForm.register')) }
            </Button>
            <div class="text-center text-[14px] mt-[12px] text-[#000000D9] dark:text-[#FFFFFFD9]">
              {i18next.t('authForm.hasCasdoorAccount')}
              <Button type="link" class="!m-0 !p-0 h-auto" onClick={() => handleEvents && handleEvents('switchToLogin')}>
                {i18next.t('authForm.loginNow')}
              </Button>
            </div>
          </div>
        ),
      }
      break
    case 'enterprise_casdoor': // casdoor없음버전 버전없음회원가입공가능
      break
    default:
      break
  }
  return conf
}

// 통신경과휴대폰 번호검증인증코드돌아가기비밀번호테이블단일매칭, saas버전uap인증시스템있음돌아가기비밀번호공가능
export const forgotPasswordFormConfig: FormConfig = {
  layout: 'vertical',
  fields: [
    fieldFactories.phone(),
    {
      ...fieldFactories.captcha(),
      sendCaptcha: async (phone: string) => {
        await sendCaptcha(phone, 'set_password', false)
      },
    },
  ],
  actionsRender: ({ handleEvents }) => (
    <div class="w-full absolute bottom-0">
      <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')}>
        {i18next.t('authForm.nextStep')}
      </Button>
    </div>
  ),
}

// 통신경과휴대폰 번호검증인증코드비밀번호테이블단일, saas버전uap인증시스템있음휴대폰 번호검증인증코드비밀번호공가능
export function createSetPasswordFormConfig(formData: any, isInvite: boolean): FormConfig {
  return {
    layout: 'vertical',
    fields: [
      fieldFactories.password(),
      fieldFactories.confirmPassword(formData),
      {
        type: 'slot',
        key: 'tip',
        customRender: () => {
          return (
            <div class="text-[14px] text-[#000000A6] dark:text-[#FFFFFFD9] mt-[12px] mb-[20px]">
              {i18next.t('authForm.passwordComplexityTip')}
            </div>
          )
        },
      },
    ],

    actionsRender: ({ handleEvents }) => (
      <div class="w-full absolute bottom-0">
        <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')}>
          { isInvite ? i18next.t('authForm.completeAndJoin') : i18next.t('authForm.complete') }
        </Button>
      </div>
    ),
  }
}

// 통신경과휴대폰 번호비밀번호수정비밀번호테이블단일, 버전uap인증시스템있음휴대폰 번호비밀번호수정비밀번호공가능
export function modifyPasswordFormConfig(formData: any, isInvite: boolean): FormConfig {
  return {
    layout: 'vertical',
    fields: [
      // { ...fieldFactories.account(), key: 'loginName' },
      { ...fieldFactories.phone(), placeholder: i18next.t('authForm.accountPlaceholder') },
      { ...fieldFactories.password(true), key: 'oldPassword' },
      { ...fieldFactories.password(), key: 'newPassword', placeholder: i18next.t('authForm.passwordPlaceholder') },
      { ...fieldFactories.confirmPassword(formData, 'newPassword'), placeholder: i18next.t('authForm.confirmPasswordPlaceholder') },
      {
        type: 'slot',
        key: 'tip',
        customRender: () => {
          return (
            <div class="text-[14px] text-[#000000A6] dark:text-[#FFFFFFD9] mt-[12px] mb-[20px]">
              {i18next.t('authForm.passwordComplexityTip')}
            </div>
          )
        },
      },
    ],

    actionsRender: ({ handleEvents }) => (
      <div class="w-full absolute bottom-0">
        <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')}>
          { isInvite ? i18next.t('authForm.completeAndJoin') : i18next.t('authForm.complete') }
        </Button>
      </div>
    ),
  }
}

// 문의테이블단일매칭, saas버전uap인증시스템있음버전회원가입문의공가능
export function consultFormConfig(consultType: 'renewal' | 'consult' = 'consult', consultEdition: 'professional' | 'enterprise'): FormConfig {
  if (consultType === 'renewal') {
    // 문의테이블단일매칭
    return {
      layout: 'vertical',
      fields: [
        fieldFactories.companyName(),
        {
          ...fieldFactories.phone(),
          key: 'mobile',
          placeholder: i18next.t('authForm.enterMobileOrManager'),
        },
        fieldFactories.renewalDuration(consultEdition),
      ],
      actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
        <div class="w-full absolute bottom-0">
          <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
            {loading ? i18next.t('authForm.submitting') : i18next.t('authForm.submitApplication')}
          </Button>
          <div class="text-center text-[14px] mt-[12px] text-[#00000040] dark:text-[#FFFFFFD9]">
            {i18next.t('authForm.contactLater')}
          </div>
        </div>
      ),
    }
  }
  return {
    layout: 'vertical',
    fields: [
      {
        ...fieldFactories.loginName(),
        key: 'contactName',
      },
      fieldFactories.companyName(),
      fieldFactories.teamSize(),
      {
        ...fieldFactories.phone(),
        key: 'mobile',
        placeholder: i18next.t('authForm.enterMobileOrManager'),
      },
      fieldFactories.email(),
    ],
    actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
      <div class="w-full absolute bottom-0">
        <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
          {loading ? i18next.t('authForm.submitting') : i18next.t('authForm.submitApplication')}
        </Button>
        <div class="text-center text-[14px] mt-[12px] text-[#000000D9] dark:text-[#FFFFFFD9]">
          {i18next.t('authForm.contactLater')}
        </div>
      </div>
    ),
  }
}

// 계정로그인테이블단일매칭
export function inviteUserInfoFormConfig(): FormConfig {
  return {
    fields: [
      { ...fieldFactories.loginName(), key: 'name', placeholder: i18next.t('authForm.loginNamePlaceholder'), disabled: () => true },
      { ...fieldFactories.phone(), placeholder: i18next.t('authForm.accountPlaceholder'), disabled: () => true },
      { ...fieldFactories.agreement(), disabled: () => true },
    ],
    actionsRender: ({ handleEvents, loading }: { handleEvents?: (event: string) => void, loading?: boolean }) => (
      <div class="w-full absolute bottom-0">
        <Button type="primary" size="large" block onClick={() => handleEvents && handleEvents('submit')} loading={loading}>
          {i18next.t('authForm.confirmJoin')}
        </Button>
        <div class="text-center text-[14px] mt-[12px] text-[#000000D9] dark:text-[#FFFFFFD9]">
          <Button type="link" class="!m-0 !p-0 !border-0 h-auto" onClick={() => handleEvents && handleEvents('switchToLogin')}>
            {i18next.t('authForm.useOtherAccount')}
          </Button>
        </div>
      </div>
    ),
  }
}