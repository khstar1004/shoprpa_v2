import type { IWorkbookData } from '@univerjs/core'

async function loadLuckyExcel() {
  const mod = await import('./luckyexcel')
  return mod.default
}

function waitUserSelectExcelFile(params: {
  onSelect?: (result: File) => void
  onCancel?: () => void
  onError?: (error: any) => void
  accept?: string
}) {
  const { onSelect, onCancel, onError, accept = '.csv' } = params
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = accept
  input.click()
  input.oncancel = () => {
    onCancel?.()
  }
  input.onchange = () => {
    const file = input.files?.[0]
    if (!file)
      return
    onSelect?.(file)
  }
  input.onerror = (error) => {
    onError?.(error)
  }
}

async function importExcelFile() {
  return new Promise<IWorkbookData>((resolve, reject) => {
    try {
      waitUserSelectExcelFile({
        accept: '.xlsx,.xls,.csv',
        onSelect: async (file: File) => {
          try {
            const LuckyExcel = await loadLuckyExcel()
            const name = file.name || ''
            const isCsv = /\.csv$/i.test(name) || file.type === 'text/csv'

            if (isCsv) {
              LuckyExcel.transformCsvToUniver(file, resolve, reject)
            }
            else {
              LuckyExcel.transformExcelToUniver(file, resolve, reject)
            }
          }
          catch (error) {
            reject(error)
          }
        },
      })
    }
    catch (error) {
      reject(error)
    }
  })
}

async function exportToExcelFile(snapshot: IWorkbookData, fileName?: string) {
  const LuckyExcel = await loadLuckyExcel()
  return new Promise<ArrayBuffer>((resolve, reject) => {
    LuckyExcel.transformUniverToExcel({
      snapshot,
      fileName,
      getBuffer: true,
      success: buffer => buffer ? resolve(buffer) : reject(new Error('No buffer returned')),
      error: error => reject(error),
    })
  })
}

async function exportToCsvFile(snapshot: IWorkbookData, fileName?: string) {
  const LuckyExcel = await loadLuckyExcel()
  return new Promise<string | { [key: string]: string }>((resolve, reject) => {
    LuckyExcel.transformUniverToCsv({
      snapshot,
      fileName,
      getBuffer: true,
      success: csvContent => csvContent ? resolve(csvContent) : reject(new Error('No csv content returned')),
      error: error => reject(error),
    })
  })
}

async function transformExcelToUniver(file: File) {
  const LuckyExcel = await loadLuckyExcel()
  return new Promise<IWorkbookData>((resolve, reject) => {
    LuckyExcel.transformExcelToUniver(file, resolve, reject)
  })
}

export const sheetUtils = {
  waitUserSelectExcelFile,
  importExcelFile,
  exportToExcelFile,
  exportToCsvFile,
  transformExcelToUniver,
}
