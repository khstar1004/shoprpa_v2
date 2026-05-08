/**
 * 전체영역데이터업데이트가져오기 의프로세스, 매칭업데이트
 * 전역 변수, 요소대기
 */
import { useFlowStore } from '@/stores/useFlowStore'
import { getProjectAllFlow } from '@/views/Arrange/utils/flowUtils'

export function useGlobalDataUpdate() {
  /**
   * 요소이름 변경
   * @param element 요소
   */
  const elementRenameAndUpdateFlow = (element) => {
    const { allFlowList } = getProjectAllFlow()
    const worker = new Worker(new URL('@/worker/index.ts', import.meta.url))
    worker.postMessage({
      key: 'flowDataElementUpdate',
      params: {
        element: {
          elementId: element.elementId,
          name: element.name,
        },
        currentFlowData: JSON.stringify(allFlowList),
        type: 'rename',
      },
    })
    worker.onmessage = (e) => {
      const { key, params } = e.data
      if (key === 'flowDataElementUpdate') {
        useFlowStore().updataOriginFlowData(params)
        worker.terminate()
      }
    }
  }
  /**
   * 삭제요소
   * @param element 요소
   */
  const elementDeleteAndUpdateFlow = (element) => {
    const { allFlowList } = getProjectAllFlow()
    const worker = new Worker(new URL('@/worker/index.ts', import.meta.url))
    worker.postMessage({
      key: 'flowDataElementUpdate',
      params: {
        element: {
          elementIds: element.elementIds,
        },
        currentFlowData: JSON.stringify(allFlowList),
        type: 'delete',
      },
    })
    worker.onmessage = (e) => {
      const { key, params } = e.data
      if (key === 'flowDataElementUpdate') {
        useFlowStore().updataOriginFlowData(params)
        worker.terminate()
      }
    }
  }

  /**
   * 본프로세스사용의모든요소
   */
  const elementUsedInFlow = () => {
    return new Promise((resolve, reject) => {
      try {
        const worker = new Worker(new URL('@/worker/index.ts', import.meta.url))
        worker.postMessage({
          key: 'elementUsedInFlow',
          params: {
            currentFlowData: useFlowStore().simpleFlowUIData,
          },
        })
        worker.onmessage = (e) => {
          const { key, params } = e.data
          if (key === 'elementUsedInFlow') {
            const { usedElementsIds } = params
            resolve(usedElementsIds)
          }
        }
      }
      catch (error) {
        reject(error)
      }
    })
  }

  return {
    elementRenameAndUpdateFlow,
    elementDeleteAndUpdateFlow,
    elementUsedInFlow,
  }
}
