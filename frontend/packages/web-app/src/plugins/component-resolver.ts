import type { ComponentResolver } from 'unplugin-vue-components/types'

/**
 * 지정컴포넌트파싱기기, 사용 a-modal 로 GlobalModal
 */
export function ModalReplacementResolver(): ComponentResolver {
  return {
    type: 'component',
    resolve: (name: string) => {
      // 까지 AModal 또는 a-modal 시, 반환 GlobalModal 의가져오기정보
      if (name === 'AModal') {
        return {
          name: 'default',
          from: '@/components/GlobalModal/index.vue',
        }
      }
    },
  }
}
