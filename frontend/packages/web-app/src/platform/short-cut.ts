import type { ShortCutManager } from '@rpa/shared/platform'

const ShortCut: ShortCutManager = {
  register(_shortKey: string, _handler: any) {},

  unregister(_shortKey: string) {},

  unregisterAll() {},

  registerToolbar() {},

  registerFlow() {},

  regeisterToolbar() {
    this.registerToolbar()
  },

  regeisterFlow() {
    this.registerFlow()
  },
}

export default ShortCut
