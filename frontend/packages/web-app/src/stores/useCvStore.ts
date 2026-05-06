import { message } from 'ant-design-vue'
// 요소정보
import { defineStore } from 'pinia'
import type { Ref } from 'vue'
import { ref } from 'vue'

import { addBase64Header, base64ToFile } from '@/utils/common'
import BUS from '@/utils/eventBus'

import { addElement, addElementGroup, delElementGroup, generateCvElementName, getElementDetail, getElementsAll, moveElement, postDeleteElement, renameElementGroup, updateElement, uploadFile } from '@/api/resource'
import { useProcessStore } from '@/stores/useProcessStore'
import type { Element, ElementGroup, PickStepType } from '@/types/resource.d'
import { useGlobalDataUpdate } from '@/views/Arrange/hook/useGlobalDataUpdate'
import { gainUnUseQuote } from '@/views/Arrange/hook/useQuoteManage'

export const useCvStore = defineStore('cv', () => {
  const processStore = useProcessStore()
  const cvTreeData = ref<ElementGroup[]>([])
  const currentCvItem = ref<Element | null>(null)
  let isNewElement = false
  const { elementDeleteAndUpdateFlow, elementRenameAndUpdateFlow } = useGlobalDataUpdate()

  // 가져오기cv선택데이터
  const getCvTreeData = async () => {
    getElementsAll({ robotId: processStore.project.id, elementType: 'cv' }).then((res: any) => {
      cvTreeData.value = res.data.map((i) => {
        if (i.open === undefined)
          i.open = true
        return i
      })
    })
  }

  // 추가분그룹
  const addGroup = (groupName: string) => {
    return new Promise((resolve, reject) => {
      addElementGroup({ robotId: processStore.project.id, groupName, elementType: 'cv' }).then((res) => {
        console.log(res)
        message.success('추가되었습니다.')
        updateCvTreeData()
        resolve(true)
      }).catch((err) => {
        reject(err)
      })
    })
  }

  // 분그룹이름 변경
  const renameGroup = (groupId: string, groupName: string) => {
    return new Promise((resolve, reject) => {
      renameElementGroup({ robotId: processStore.project.id, groupId, groupName, elementType: 'cv' }).then((res) => {
        console.log(res)
        message.success('이름이 변경되었습니다.')
        updateCvTreeData()
        resolve(true)
      }).catch((err) => {
        reject(err)
      })
    })
  }

  // 삭제분그룹
  const deleteGroup = (groupId: string) => {
    const delElments = cvTreeData.value.find(i => i.id === groupId).elements
    delElementGroup({ robotId: processStore.project.id, groupId }).then(() => {
      message.success('삭제되었습니다.')
      updateCvTreeData()
      // 업데이트프로세스데이터-삭제해당분그룹아래의이미지
      elementDeleteAndUpdateFlow({ elementIds: delElments.map(i => i.id) })
    })
  }

  // 업데이트분그룹
  const updateCvTreeData = () => {
    getCvTreeData()
  }

  const getCvItemDetail = async (elementId: string) => {
    const { data } = await getElementDetail({ robotId: processStore.project.id, elementId })
    isNewElement = false
    return data
  }

  const uploadImgFile = async () => {
    if (isNewElement) {
      const { id, imageUrl, parentImageUrl } = currentCvItem.value
      const fileName = `${id}.png`
      const [imageId, parentImageId] = await Promise.all([
        imageUrl ? uploadFile({ file: base64ToFile(imageUrl, fileName) }) : Promise.resolve(''),
        parentImageUrl ? uploadFile({ file: base64ToFile(parentImageUrl, `parent_${fileName}`) }) : Promise.resolve(''),
      ])
      return { imageId, parentImageId }
    }
    return {}
  }

  // 저장이미지
  const saveCvItem = async (cvParams: Element, groupId: string = '') => {
    return new Promise((resolve, reject) => {
      const robotId = processStore.project.id
      const _name = cvParams.name.replaceAll(' ', '') // 제거빈격식
      if (_name === '') {
        reject(new Error('요소 이름을 입력하세요.'))
      }

      uploadImgFile().then((res: any) => {
        const params = {
          robotId,
          groupName: groupId ? cvTreeData.value.find(i => i.id === groupId)?.name : '',
          element: {
            ...cvParams,
            name: _name,
            ...res,
            imageUrl: '',
            parentImageUrl: '',
          },
          type: 'cv',
        }
        // 선택
        const id = cvParams.id && isCVItemExsist(cvParams.id) ? cvParams.id : ''
        const reqFunc = id ? updateElement : addElement
        reqFunc(params).then((res) => {
          BUS.$emit('cv-pick-done', { data: id || res.data.elementId, value: _name }) // 알림컴포넌트선택완료완료
          updateCvTreeData() // 업데이트데이터
          if (id && currentCvItem.value?.name !== _name) { // 이름있음변수, 수정프로세스데이터중의이름
            elementRenameAndUpdateFlow({ elementId: id, name: _name })
          }
          resolve(true)
        }).catch((err) => {
          reject(err)
        })
      })
    })
  }

  // 삭제이미지
  const deleteCvItem = (cvItem: Element) => {
    postDeleteElement({ robotId: processStore.project.id, elementId: cvItem.id }).then((res) => {
      console.log(res)
      updateCvTreeData()
      // 업데이트프로세스데이터-삭제해당이미지
      elementDeleteAndUpdateFlow({ elementIds: [cvItem.id] })
    })
  }
  // 이미지까지개분그룹
  const moveCvItem = (id: string, groupId: string) => {
    moveElement({ robotId: processStore.project.id, groupId, elementId: id }).then((res) => {
      console.log(res)
      updateCvTreeData()
    })
  }

  const setCurrentCvItem = (cvItem: Element) => {
    isNewElement = false
    currentCvItem.value = cvItem
  }

  // 시이미지
  const setTempCvItem = async (data: any, pickStep: PickStepType = 'new') => {
    let element: Element
    const { img } = data
    const imageUrl = img.self ? addBase64Header(img.self) : ''
    const parentImageUrl = img.parent ? addBase64Header(img.parent) : ''
    const elementData = {
      ...data,
      img: {
        // cv이미지, 저장후이면로이미지주소
        self: '',
        parent: '',
      },
      defaultAnchor: pickStep !== 'anchor',
    }

    if (pickStep === 'new') {
      // 새생성요소, name, elementId, img 필요완료
      // const name = generateName(`이미지_1`)
      const data = await generateCvElementName({ robotId: processStore.project.id })
      const name = data.data
      // const elementId = genNonDuplicateID('cv')
      element = {
        name,
        // id: elementId,
        imageUrl,
        parentImageUrl,
        elementData,
      }
    }
    else if (pickStep === 'repick') {
      // 다시 선택요소, 이미지,  elementData 필요
      element = { ...currentCvItem.value, imageUrl, parentImageUrl, elementData }
    }
    else if (pickStep === 'anchor') {
      //  currentElement.value.parentImageUrl, currentElement.value.elementData elementData중의pos필요
      const selfPos = JSON.parse(currentCvItem.value.elementData).pos
      elementData.pos = { ...data.pos, self_x: selfPos.self_x, self_y: selfPos.self_y }
      element = { ...currentCvItem.value, parentImageUrl, elementData }
    }
    element.elementData = JSON.stringify(elementData)
    setCurrentCvItem(element)
    isNewElement = true
  }

  const quotedItem = ref(null)

  const setQuotedItem = (item?: Element) => {
    quotedItem.value = item || null
  }

  const getUnUseTreeData = (unuseTreeData: Ref, unUseNum: Ref, type: string) => {
    gainUnUseQuote((useImageIds) => {
      unuseTreeData.value = cvTreeData.value.map((item) => {
        return {
          ...item,
          elements: item.elements.filter(i => !useImageIds.includes(i.id)),
        }
      }).filter(i => i.elements.length > 0)
      unUseNum.value = 0
      unuseTreeData.value.forEach((item) => {
        unUseNum.value += item.elements.length
      })
    }, type)
  }

  const isCVItemExsist = (id: string) => {
    return cvTreeData.value.some(i => i.elements.some(j => j.id === id))
  }

  const resetCurrentItem = () => {
    setCurrentCvItem(null)
    isNewElement = false
  }

  return {
    cvTreeData,
    getCvTreeData,
    updateCvTreeData,
    addGroup,
    renameGroup,
    deleteGroup,
    getCvItemDetail,
    saveCvItem,
    deleteCvItem,
    moveCvItem,
    currentCvItem,
    setCurrentCvItem,
    setTempCvItem,
    quotedItem,
    setQuotedItem,
    getUnUseTreeData,
    resetCurrentItem,
    isCVItemExsist,
  }
})
