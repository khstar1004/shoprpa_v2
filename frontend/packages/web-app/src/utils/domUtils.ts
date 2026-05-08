/**
 * 요소 name까지
 * @param name 필요 name 여부있음의전
 */
export function scrollToName(name: string, times?: number) {
  if (!name)
    return
  if (times <= 0) {
    return
  }
  else {
    times--
  }
  const element = document.getElementsByName(name)
  if (element && element[0]) {
    setTimeout(() => { // 중지미완료성공, 지연300ms
      element[0].scrollIntoView({
        block: 'center',
      })
    }, 300)
  }
  else {
    const ti = setTimeout(() => {
      scrollToName(name, 10)
      clearTimeout(ti)
    }, 300)
  }
}
