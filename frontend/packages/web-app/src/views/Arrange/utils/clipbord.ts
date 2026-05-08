export function setClipBoardData(projectId: string | number, atoms: any = null, type: string) {
  localStorage.setItem('clipBoardData', JSON.stringify({
    projectId,
    atoms,
    type,
  }))
}

export function getClipBoardData(callback) {
  const clipBoardData = JSON.parse(localStorage.getItem('clipBoardData') || '{}')
  callback(clipBoardData)
}

// 출력사용시, 빈시파일의데이터
export function clearClipBoardData() {
  // 빈파일
  localStorage.removeItem('clipBoardData')
}
