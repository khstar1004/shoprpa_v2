import i18next from '@/plugins/i18next'

import safe360Icon from '@/assets/img/pluginInstall/360.png'
import x360Icon from '@/assets/img/pluginInstall/360x.png'
import edgeIcon from '@/assets/img/pluginInstall/edge-icon.png'
import firefoxIcon from '@/assets/img/pluginInstall/firefox.png'
import googleIcon from '@/assets/img/pluginInstall/google-icon.png'

export interface PLUGIN_ITEM {
  type: BROWSER_LIST
  icon: string
  title: string
  isInstall: boolean
  installVersion: string
  description: string
  isNewest?: boolean
  oparateStepImgs?: string[]
  stepDescription?: string[]
  loading?: boolean
  browserInstalled?: boolean
}

export enum BROWSER_LIST {
  'CHROME' = 'chrome',
  'EDGE' = 'microsoft_edge',
  'FIREFOX' = 'firefox',
  '360SE' = '360',
  '360X' = '360x',
}
export { BROWSER_LIST as BROWER_LIST }

// 확장목록매칭
export const BROWSER_PLUGIN_LIST: PLUGIN_ITEM[] = [
  {
    type: BROWSER_LIST.CHROME, // chrome프로그램
    icon: googleIcon,
    title: 'chrome',
    isInstall: false,
    installVersion: '',
    description: 'chromeDescription',
    isNewest: true,
    stepDescription: [i18next.t('plugin.step1'), i18next.t('plugin.step2')], // 설치후의설명
    oparateStepImgs: ['chrome1.jpg', 'chrome2.jpg'], // 설치후의이미지
    loading: false, // 여부정상에서설치
  },
  {
    type: BROWSER_LIST.EDGE, // edge프로그램
    icon: edgeIcon,
    title: 'edge',
    isInstall: false,
    installVersion: '',
    description: 'edgeDescription',
    isNewest: true,
    stepDescription: [i18next.t('plugin.step1'), i18next.t('plugin.step2')], // 설치후의설명
    oparateStepImgs: ['edge1.jpg', 'edge2.jpg'], // 설치후의이미지
    loading: false, // 여부정상에서설치
  },
  {
    type: BROWSER_LIST.FIREFOX, // firefox프로그램
    icon: firefoxIcon,
    title: 'firefox',
    isInstall: false,
    installVersion: '',
    description: 'firefoxDescription',
    isNewest: true,
    loading: false, // 여부정상에서설치
  },
  {
    type: BROWSER_LIST['360SE'], // 360프로그램
    icon: safe360Icon,
    title: '360',
    isInstall: false,
    installVersion: '',
    description: '360Description',
    isNewest: true,
    loading: false, // 여부정상에서설치
  },
  {
    type: BROWSER_LIST['360X'], // 360x프로그램
    icon: x360Icon,
    title: '360x',
    isInstall: false,
    installVersion: '',
    description: '360xDescription',
    isNewest: true,
    loading: false, // 여부정상에서설치
  },
]
export { BROWSER_PLUGIN_LIST as BROWER_PLUGIN_LIST }
