import GlobalModal from '@/components/GlobalModal/index.ts'
/**
 * 새로고침팝업
 */
export function refreshModal() {
  GlobalModal.confirm({
    title: '안내',
    content: '감지까지완료업데이트, 여부새로고침페이지?',
    onOk: () => {
      window.location.reload()
    },
    // onCancel: () => {
    //   refreshPlugin();
    // }
    centered: true,
    keyboard: false,
  })
}