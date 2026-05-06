import { TinyColor } from '@ctrl/tinycolor'
import { theme } from 'ant-design-vue'

const { defaultSeed, defaultAlgorithm, darkAlgorithm } = theme

const white = '#FFFFFF'
const black = '#000000'
const colorPrimary = '#0EA5A8'
const darkColorPrimay = new TinyColor('#2DD4BF').setAlpha(0.95).toString()

const seeds = {
  ...defaultSeed,
  colorPrimary,
}

const defaultTokens = {
  ...defaultAlgorithm(seeds),
  colorPrimaryBg: 'rgba(14,165,168,0.12)',
  colorBgElevated: white,
  colorText: new TinyColor(black).setAlpha(0.85).toString(),
  colorLink: '#0EA5A8',
  colorLinkHover: '#0F766E',
}
const darkTokens = {
  ...darkAlgorithm(seeds),
  colorPrimary: darkColorPrimay,
  colorPrimaryBg: 'rgba(45,212,191,0.14)',
  colorBgElevated: '#151b22',
  colorText: white,
  colorLink: '#2dd4bf',
  colorLinkHover: '#5eead4',
}

const presetColors = [
  'blue',
  'purple',
  'cyan',
  'green',
  'magenta',
  'pink',
  'red',
  'orange',
  'yellow',
  'volcano',
  'geekblue',
  'lime',
  'gold',
]

// 에서 1 까지 10 의색상정도배열
const gradientInde = Array.from({ length: 10 }, (_, i) => i + 1)

const colorTokenNames = [
  ...presetColors.map(it => [it, ...gradientInde.map(index => `${it}-${index}`)]).flat(),
  'colorBgBase',
  'colorError',
  'colorInfo',
  'colorPrimary',
  'colorSuccess',
  'colorTextBase',
  'colorWarning',
  'colorBgContainer',
  'colorBgElevated',
  'colorBgLayout',
  'colorBgMask',
  'colorBgSpotlight',
  'colorBorder',
  'colorBorderSecondary',
  'colorErrorActive',
  'colorErrorBg',
  'colorErrorBgHover',
  'colorErrorBorder',
  'colorErrorBorderHover',
  'colorErrorHover',
  'colorErrorText',
  'colorErrorTextActive',
  'colorErrorTextHover',
  'colorFill',
  'colorFillQuaternary',
  'colorFillSecondary',
  'colorFillTertiary',
  'colorInfoActive',
  'colorInfoBg',
  'colorInfoBgHover',
  'colorInfoBorder',
  'colorInfoBorderHover',
  'colorInfoHover',
  'colorInfoText',
  'colorInfoTextActive',
  'colorInfoTextHover',
  'colorPrimaryActive',
  'colorPrimaryBg',
  'colorPrimaryBgHover',
  'colorPrimaryBorder',
  'colorPrimaryBorderHover',
  'colorPrimaryHover',
  'colorPrimaryText',
  'colorPrimaryTextActive',
  'colorPrimaryTextHover',
  'colorSuccessActive',
  'colorSuccessBg',
  'colorSuccessBgHover',
  'colorSuccessBorder',
  'colorSuccessBorderHover',
  'colorSuccessHover',
  'colorSuccessText',
  'colorSuccessTextActive',
  'colorSuccessTextHover',
  'colorText',
  'colorTextQuaternary',
  'colorTextSecondary',
  'colorTextTertiary',
  'colorWarningActive',
  'colorWarningBg',
  'colorWarningBgHover',
  'colorWarningBorder',
  'colorWarningBorderHover',
  'colorWarningHover',
  'colorWarningText',
  'colorWarningTextActive',
  'colorWarningTextHover',
  'colorWhite',
]

const tokens = Object.entries(defaultTokens).reduce((result, [key, value]) => {
  const isColor = colorTokenNames.includes(key)
  const isDifferent = isColor && darkTokens[key] !== value

  result[key] = {
    value,
    type: isColor ? 'color' : undefined,
    dark: isDifferent ? darkTokens[key] : undefined,
  }
  return result
}, {})

export default tokens
