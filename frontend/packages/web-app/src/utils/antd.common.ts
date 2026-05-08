import GlobalModal from '@/components/GlobalModal/index.ts'
/**
 * 새로고침팝업
 */
export function refreshModal() {
  GlobalModal.confirm({
    title: '안내',
    content: '업데이트가 완료되었습니다. 페이지를 새로고침하시겠습니까?',
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
