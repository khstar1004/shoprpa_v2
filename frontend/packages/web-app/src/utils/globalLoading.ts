import BUS from '@/utils/eventBus'

const $loading = {
  /**
   * @param options.msg loading 안내정보
   * @param options.timeout  시간 단일위치초
   * @param options.exit 여부출력버튼
   * @param options.exitCallback 출력버튼돌아가기조정
   */
  open: (options: { msg?: string, timeout?: number, exit?: boolean, exitCallback?: () => void }) => {
    const { msg = '', timeout = 200, exit = false, exitCallback } = options
    BUS.$emit('isLoading', { isLoading: true, text: msg, timeout, exit, exitCallback })
  },
  /**
   * @param immediate 여부닫기
   */
  close: (immediate?: boolean) => {
    BUS.$emit('isLoading', { isLoading: false, immediate })
  },
}
export default $loading