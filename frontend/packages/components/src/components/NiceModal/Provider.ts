import { computed, defineComponent, h } from 'vue'

import { ALREADY_MOUNTED, MODAL_REGISTRY } from './contants'
import { useModalContext } from './store'

export const Provider = defineComponent({
  name: 'NiceModalProvider',
  setup(_, { slots }) {
    return () => [slots.default?.(), h(NiceModalPlaceholder)]
  },
})

// Modal placeholder component for auto rendering modals
const NiceModalPlaceholder = defineComponent({
  name: 'NiceModalPlaceholder',
  setup() {
    const { state } = useModalContext()

    // 가져오기모든필요의ID
    const visibleModalIds = computed(() => {
      return Object.keys(state.value).filter(id => !!state.value[id])
    })

    visibleModalIds.value.forEach((id) => {
      if (!MODAL_REGISTRY[id] && !ALREADY_MOUNTED[id]) {
        console.warn(
          `No modal found for id: ${id}. Please check the id or if it is registered or declared via JSX.`,
        )
      }
    })

    return () => {
      const toRender = visibleModalIds.value
        .filter(id => MODAL_REGISTRY[id])
        .map((id) => {
          const registry = MODAL_REGISTRY[id]
          return {
            id,
            comp: registry.comp,
            props: registry.props || {},
          }
        })

      return toRender.map(t => h(t.comp, { key: t.id, id: t.id, ...t.props }))
    }
  },
})