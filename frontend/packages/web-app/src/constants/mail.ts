import i18next from '@/plugins/i18next'

export const EMAIL_OPTIONS = [
  {
    label: '163',
    value: '163Email',
  },
  {
    label: '126',
    value: '126Email',
  },
  {
    label: 'QQ',
    value: 'qqEmail',
  },
  {
    label: i18next.t('mail.otherEmail'),
    value: 'customEmail',
  },
]

// 메일 서비스 값을 백엔드 플래그로 변환한다.
export const EMAIL_OPTIONS_MAP = {
  // `qq`, `163`, `126`, `shoprpa`, `advance`
  'customEmail': 'advance',
  'advance': 'advance',
  '126Email': '126',
  '126': '126',
  '163Email': '163',
  '163': '163',
  'qqEmail': 'qq',
  'qq': 'qq',
  'shoprpaEmail': 'shoprpa',
  'shoprpa': 'shoprpa',
}

export const PROTOCAL_OPTIONS = [
  {
    label: 'IMAP',
    value: 'IMAP',
  },
  {
    label: 'POP3',
    value: 'POP3',
  },
]
