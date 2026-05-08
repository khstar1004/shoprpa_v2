import { ref, toRaw } from 'vue'

import { isBase64Image, trimBase64Header } from '@/utils/common'

import type { ElementsType } from '@/types/resource'
import { VISUALIZATION } from '@/views/Arrange/config/pick'
import { elementCustomFormat, elementCustomFormatRecover, elementDirectoryFormat, elementDirectoryFormatRecover } from '@/views/Arrange/utils/elementsUtils'

/**
 * 가져오기현재요소새데이터
 * 관리부서분데이터형식
 * @param currentElement 현재요소객체
 * @param isSave 여부로저장
 * @returns 관리후의요소데이터
 */
export function getLatestCurrentElementData(currentElement: ElementsType, isSave: boolean) {
  const detailElementData = ref(JSON.parse(currentElement.elementData))
  const { version, type, path } = detailElementData.value
  const customData = ref(elementCustomFormat(version, type, path))
  const nodeSourceData = ref(elementDirectoryFormat(version, type, path))
  const { checkType, matchTypes } = path || {}
  const formOption = ref({
    matchTypes: matchTypes || [],
    editXPathType: checkType || VISUALIZATION,
  })

  let elementPathData
  let elementData
  if (type === 'web') {
    const customDataMap = toRaw(
      elementCustomFormatRecover(version, type, customData.value),
    ) // url, xpath, cssSelector
    const pathDirs = toRaw(
      elementDirectoryFormatRecover(version, type, nodeSourceData.value),
    ) // pathDirs
    elementPathData = {
      ...detailElementData.value.path,
      ...customDataMap,
      pathDirs,
      checkType: formOption.value.editXPathType,
      matchTypes: formOption.value.matchTypes,
    }
    if (isSave) {
      // 삭제아니오필요의데이터
      'img' in detailElementData.value && delete detailElementData.value.img
      'similarCount' in elementPathData && delete elementPathData.similarCount
      'rect' in elementPathData && delete elementPathData.rect
    }
    elementData = {
      ...detailElementData.value, // 요소외부데이터
      path: elementPathData, // 요소경로데이터
    }
  }
  else {
    elementPathData = elementDirectoryFormatRecover(
      version,
      type,
      nodeSourceData.value,
    ) // path 데이터
    const selfImg = currentElement.imageUrl
    const parentImg = currentElement.parentImageUrl
    // 저장시예base64이미지, 이면아니오저장base64
    const img = isSave
      ? {
          self: isBase64Image(selfImg) ? '' : selfImg,
          parent: isBase64Image(parentImg) ? '' : parentImg,
        }
      : {
          self: isBase64Image(selfImg) ? trimBase64Header(selfImg) : selfImg,
          parent: isBase64Image(parentImg)
            ? trimBase64Header(parentImg)
            : parentImg,
        }
    elementData = {
      ...detailElementData.value, // 요소데이터
      img,
      path: elementPathData,
    }
    if (elementData.picker_type === 'SIMILAR') {
      // uia 정도요소, 저장시변수변경 필드
      elementData.img = {
        self: isBase64Image(selfImg) ? '' : selfImg,
        parent: isBase64Image(parentImg) ? '' : parentImg,
      }
      elementData.similar_count ? delete elementData.similar_count : ''
    }
  }
  return elementData
}
