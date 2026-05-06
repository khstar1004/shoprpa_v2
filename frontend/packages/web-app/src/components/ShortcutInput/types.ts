export interface ShortcutItemMap {
  id: string
  name: string
  value: string
  text: string
  validate?: ''
  active?: false
  showSettingCenter?: true // 여부에서중
}

export interface ShortcutMap {
  [key: string]: ShortcutItemMap
}