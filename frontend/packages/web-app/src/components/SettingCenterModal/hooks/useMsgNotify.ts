import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { useTranslation } from 'i18next-vue'
import { isEmpty } from 'lodash-es'
import { onBeforeUnmount, ref } from 'vue'
import type { Ref } from 'vue'

import { toolsInterfacePost } from '@/api/setting'
import useUserSettingStore from '@/stores/useUserSetting.ts'

function initEmailData(): RPA.EmailFormMap {
  return {
    is_enable: false, // 여부사용, 아니오사용
    receiver: '', // 파일사람
    is_default: true, // 아니오사용메일함
    mail_server: '', // 발송파일서비스기기
    mail_port: '', // 단말
    sender_mail: '', // 메일계정
    password: '', // 메일비밀번호(필요사용키, 저장의예키이름)
    use_ssl: true, // 여부SSL
    cc: '', //
  }
}

function initPhoneData(): RPA.PhoneFormMap {
  return {
    is_enable: false, // 여부사용, 아니오사용
    receiver: '', // 파일사람휴대폰 번호
    phone_msg_url: '',
  }
}

export function useNotify() {
  const { t } = useTranslation()
  const emailRef = ref()

  const email: Ref<RPA.EmailFormMap> = ref(initEmailData())
  const emailFormRules: Record<string, Rule[]> = {
    receiver: [
      { required: true, message: t('userForm.enterEmail'), trigger: 'blur' },
      {
        pattern: /\w[-\w.+]*@([A-Z0-9][-A-Z0-9]+\.)+[A-Z]{2,14}/i,
        message: t('settingCenter.msgNotify.mailFormatError'),
        trigger: 'blur',
      },
    ],
    mail_server: [{ required: true, message: t('settingCenter.msgNotify.inputMailServer'), trigger: 'blur' }],
    mail_port: [{ required: true, message: t('settingCenter.msgNotify.inputMailPort'), trigger: 'blur' }],
    sender_mail: [
      { required: true, message: t('settingCenter.msgNotify.inputSenderMail'), trigger: 'blur' },
      {
        pattern: /\w[-\w.+]*@([A-Z0-9][-A-Z0-9]+\.)+[A-Z]{2,14}/i,
        message: t('settingCenter.msgNotify.mailFormatError'),
        trigger: 'blur',
      },
    ],
    password: [{ required: true, message: t('settingCenter.msgNotify.inputSenderPassword'), trigger: 'blur' }],
  }
  const phoneRef = ref()
  const phone_msg: Ref<RPA.PhoneFormMap> = ref(initPhoneData())
  const phoneFormRules: Record<string, Rule[]> = {
    receiver: [
      // /0?(13|14|15|18)[0-9]{9}/
      { required: true, message: t('userForm.enterPhone'), trigger: 'blur' },
      {
        pattern: /^\+?\d[0-9\s-]{6,18}\d$/,
        message: t('settingCenter.msgNotify.phoneFormatError'),
        trigger: 'blur',
      },
    ],
  }

  function handleMsgTest(key: string) {
    handleValidateSave().then(() => {
      toolsInterfacePost({
        alert_type: key,
      }).then((res) => {
        message.success(res.msg || t('settingCenter.msgNotify.testSuccess'))
      })
      message.info(t('settingCenter.msgNotify.testSent', { type: key === 'mail' ? t('userForm.email') : t('userForm.phone') }))
    })
  }
  function errorSave() {
    let newSetting
    if (email.value.is_enable) {
      newSetting = {
        msgNotifyForm: {
          email: initEmailData(),
          phone_msg: phone_msg.value,
        },
      }
    }
    else {
      newSetting = {
        msgNotifyForm: {
          email: email.value,
          phone_msg: initPhoneData(),
        },
      }
    }
    useUserSettingStore().saveUserSetting(newSetting)
  }
  function handleValidateSave() {
    return new Promise((resolve, reject) => {
      const currRef = email.value.is_enable ? emailRef : phoneRef
      currRef.value.validate().then(() => {
        const newSetting = {
          msgNotifyForm: {
            email: email.value,
            phone_msg: phone_msg.value,
          },
        }
        useUserSettingStore().saveUserSetting(newSetting)
        resolve({})
      }).catch(() => {
        reject(new Error(t('common.validationFailed')))
      })
    })
  }

  function initData() {
    const msgNotifyForm = useUserSettingStore().userSetting?.msgNotifyForm || {} as RPA.MessageFormMap
    const { email: emailData, phone_msg: phoneData } = msgNotifyForm
    if (emailData && !isEmpty(emailData)) {
      email.value = emailData
    }
    if (phoneData && !isEmpty(phoneData)) {
      phone_msg.value = {
        ...initPhoneData(),
        ...phoneData,
        phone_msg_url: phoneData.phone_msg_url?.includes('/dripsms/smssafe') ? '' : phoneData.phone_msg_url || '',
      }
    }
  }
  initData()

  onBeforeUnmount(() => {
    handleValidateSave().catch(() => { errorSave() })
  })
  return {
    emailRef,
    email,
    emailFormRules,
    phoneRef,
    phone_msg,
    phoneFormRules,
    handleValidateSave,
    handleMsgTest,
  }
}
