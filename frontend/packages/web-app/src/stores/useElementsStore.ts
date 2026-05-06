import { message } from 'ant-design-vue'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRoute } from 'vue-router'

import { addBase64Header, base64ToFile, isBase64Image } from '@/utils/common'
import BUS from '@/utils/eventBus'

// 요소정보
import {
  addElement,
  addElementGroup,
  createElementCopy,
  delElementGroup,
  getElementDetail,
  getElementsAll,
  moveElement,
  postDeleteElement,
  renameElementGroup,
  updateElement,
  uploadFile,
} from '@/api/resource'
import type { ElementsTree, ElementsType, PickElementType, PickStepType } from '@/types/resource.d'
import { useGlobalDataUpdate } from '@/views/Arrange/hook/useGlobalDataUpdate'

export const useElementsStore = defineStore('elements', () => {
  const route = useRoute()
  const elements = ref<ElementsTree[]>([])
  const { elementDeleteAndUpdateFlow, elementRenameAndUpdateFlow } = useGlobalDataUpdate()
  const selectedElement = ref<ElementsType>({
    id: '',
    name: '',
    elementData: '',
  }) // 선택중요소

  const currentElement = ref<ElementsType>({
    id: '',
    name: '',
    elementData: '',
  }) // 현재요소

  // 현재요소
  const setCurrentElement = (data: ElementsType) => {
    currentElement.value = data
  }

  // 모든요소
  const setElements = (data: ElementsTree[]) => {
    // 재그룹데이터
    elements.value = data.map((item) => {
      const elements = (item.elements ?? []).map(eleItem => ({
        ...eleItem,
        groupName: item.name, // 분그룹이름
        groupId: item.id, // 분그룹id
      }))
      return { ...item, groupName: item.name, elements }
    })
  }
  // 선택중요소
  const setSelectedElement = (data: ElementsType) => {
    selectedElement.value = data
  }
  // 재선택중요소
  const resetSelectedElement = () => {
    Object.keys(selectedElement.value).forEach((key) => {
      selectedElement.value[key] = ''
    })
    setSelectedElement(selectedElement.value)
  }
  // 재현재요소
  const resetCurrentElement = () => {
    Object.keys(currentElement.value).forEach((key) => {
      currentElement.value[key] = ''
    })
    setCurrentElement(currentElement.value)
  }
  // 요청 모든요소
  const requestAllElements = async () => {
    resetSelectedElement()
    const robotId = route.query?.projectId as string
    const { data } = await getElementsAll({ robotId, elementType: 'common' })
    if (data) {
      setElements(data)
    }
    return data || []
  }
  // 요청 까지현재요소
  const requestElementDetail = async (params: ElementsType) => {
    const robotId = route.query?.projectId as string
    const elementId = params.id
    const { data } = await getElementDetail({ robotId, elementId })
    setCurrentElement(data)
    return data
  }
  // 요청 까지현재요소
  const renameElement = async (params: ElementsType) => {
    console.log('params: ', params)
    const _name = params.name.trim()
    if (_name === '') {
      message.error('요소 이름을 입력하세요.')
      return
    }

    const robotId = route.query?.projectId as string
    await getElementDetail({ robotId, elementId: params.id })
    const res = await updateElement({
      robotId,
      element: {
        id: params.id,
        name: _name,
      },
    })
    if (res) {
      message.success('수정되었습니다.')
      await requestAllElements()
      elementRenameAndUpdateFlow({ ...params, name: _name })
    }
  }

  /**
   * 추가요소(아니오필요경과, 를선택까지의데이터직선연결업로드)
   * TODO: 부서분공가능및아래의 saveElement 재복사, 필요
   * @param data 요소데이터
   */
  const addNewElement = async (data: PickElementType) => {
    const groupName = data.app
    const robotId = route.query?.projectId as string
    const elementData = JSON.stringify(data)
    const name = generateName(data)
    const imageUrl = data.img.self ? addBase64Header(data.img.self) : ''
    const parentImageUrl = data.img.parent ? addBase64Header(data.img.parent) : ''

    const [imageId, parentImageId] = await Promise.all([
      isBase64Image(imageUrl) ? uploadFile({ file: base64ToFile(imageUrl, 'image.png') }) : '',
      isBase64Image(parentImageUrl) ? uploadFile({ file: base64ToFile(parentImageUrl, 'parent_image.png') }) : '',
    ])

    const res = await addElement({
      type: 'common',
      robotId,
      groupName,
      element: {
        commonSubType: 'single',
        name,
        icon: '',
        imageId,
        parentImageId,
        elementData,
      },
    })

    return { ...res.data, name }
  }

  // 저장현재요소
  const saveElement = async (data: PickElementType, name: string, emit = true) => {
    const robotId = route.query?.projectId as string
    const { groupName, id, imageUrl, parentImageUrl } = currentElement.value
    const elementData = JSON.stringify(data)
    const icon = '' // Array.isArray(data.elementData.path) ? "" : data.elementData.path.favIconUrl
    const isNew = isNewElement(id)
    const fileName = `${id}.png`

    const [imageId, parentImageId] = await Promise.all([
      isBase64Image(imageUrl) ? uploadFile({ file: base64ToFile(imageUrl, fileName) }) : '',
      isBase64Image(parentImageUrl) ? uploadFile({ file: base64ToFile(parentImageUrl, `parent_${fileName}`) }) : '',
    ])

    let newElement: ElementsType

    // 여부예새요소, 이면추가
    if (isNew) {
      newElement = {
        commonSubType: 'single',
        name,
        icon,
        imageId,
        parentImageId,
        elementData,
      }
      const { data } = await addElement({
        type: 'common',
        robotId,
        groupName,
        element: newElement,
      })
      newElement.id = data.elementId
      emit && BUS.$emit('pick-done', { data: data.elementId, value: name }) // 알림컴포넌트선택완료완료
    }
    else {
      // 업데이트현재요소
      const oldName = currentElement.value.name
      const elementId = currentElement.value.id
      newElement = {
        ...currentElement.value,
        name,
        elementData,
      }
      if (imageId) {
        newElement.imageId = imageId
      }
      if (parentImageId) {
        newElement.parentImageId = parentImageId
      }
      await updateElement({
        robotId,
        element: newElement,
      })
      if (oldName !== name) {
        // 이름있음변수
        elementRenameAndUpdateFlow({ elementId, name })
      }
    }
    resetCurrentElement()
    await requestAllElements()
    return newElement
  }
  // 삭제현재요소
  const deleteElement = async (data: ElementsType) => {
    const _data = { ...data }
    const robotId = route.query?.projectId as string
    await postDeleteElement({
      robotId,
      elementId: _data.id,
    })
    await requestAllElements()
    elementDeleteAndUpdateFlow({ elementIds: [_data.id] })
  }
  // 를결과변환로평면결과
  function convertTreeToFlat(tree: ElementsTree[]) {
    const flat: ElementsType[] = []
    for (const item of tree) {
      item.elements.forEach((child) => {
        flat.push(child)
      })
    }
    return flat
  }

  // 검증재이름
  function checkName(name: string, elementId?: string) {
    const flat = convertTreeToFlat(elements.value)
    const ele = flat.find(item => item.id === elementId)
    if (ele) {
      // 요소시, 정렬제거
      return flat.some(item => item.name === name && item.id !== elementId)
    }
    return flat.some(item => item.name === name)
  }

  // 완료일이름
  function generateName(data: PickElementType) {
    const { type, path } = data

    const genName = (name: string) => {
      if (checkName(name)) {
        const index = name.match(/\d+$/) ? Number.parseInt(name.match(/\d+$/)[0]) + 1 : 1
        const newName = `${name.replace(/\d+$/, '')}${index}`
        return genName(newName)
      }
      return name
    }

    // 문자열중제어문자기호, 빈격식,  문자기호
    function removeControlChars(str: string) {
      const specialChars = /[!@#$%^&*(),.?":{}|<>]/g
      return str.replace(specialChars, '').replace(/[\x00-\x1F\x7F-\x9F\s]+/g, '').replace(' ', '')
    }

    let tag = ''
    let text = ''

    if (type === 'web' && 'tag' in path) {
      tag = path.tag || '요소'
      text = path.text || '이름없음'
    }
    else if (path && Array.isArray(path)) {
      tag = path[path.length - 1].tag_name || '요소'
      text = path[path.length - 1].name || path[path.length - 1].value || '이름없음'
    }

    text = removeControlChars(text.substring(0, 10)) || '이름없음'
    return genName(`${tag}_${text}_1`)
  }

  // 검증여부예새요소
  function isNewElement(elementId: string) {
    const flat = convertTreeToFlat(elements.value)
    return flat.every(item => item.id !== elementId)
  }

  /**
   * 현재저장요소
   * @param data 요소데이터
   * @param pickStep 선택 'new' | 'repick' | 'similar
   * @returns Promise
   */
  const setTempElement = (data: PickElementType, pickStep: PickStepType = 'new', group?: string) => {
    return new Promise((resolve) => {
      const { app, type, img } = data
      const elementData = JSON.stringify({
        ...data,
        // 요소이미지, 저장후이면로이미지주소
        img: { self: '', parent: '' },
      })
      let element: ElementsType
      const groupName = group || app
      const icon = ''
      let imageUrl = ''
      let parentImageUrl = ''

      if (pickStep === 'new') {
        const name = generateName(data)
        imageUrl = img.self ? addBase64Header(img.self) : ''
        parentImageUrl = img.parent ? addBase64Header(img.parent) : ''
        element = { name, imageUrl, parentImageUrl, groupName, icon, elementData }
      }
      else if (pickStep === 'repick') {
        imageUrl = img.self ? addBase64Header(img.self) : ''
        parentImageUrl = img.parent ? addBase64Header(img.parent) : ''
        // 다시 선택요소, 이미지,  elementData 필요
        element = { ...currentElement.value, imageUrl, parentImageUrl, elementData }
      }
      else if (pickStep === 'similar') {
        if (type === 'web') {
          // 요소 currentElement.value.elementData
          element = { ...currentElement.value, elementData }
        }
        else {
          // 요소 currentElement.value.elementData
          element = { ...currentElement.value, elementData }
        }
      }
      setCurrentElement(element)
      resolve(true)
    })
  }

  // 가져오기요소의그룹
  const getParentGroup = (groupName: string) => {
    return elements.value.find(item => item.groupName === groupName)
  }

  // 추가분그룹
  const addGroup = (groupName: string) => {
    return new Promise((resolve, reject) => {
      addElementGroup({ robotId: route.query?.projectId as string, groupName, elementType: 'common' }).then(() => {
        message.success('추가되었습니다.')
        requestAllElements()
        resolve(true)
      }).catch((err) => {
        reject(err)
      })
    })
  }

  // 분그룹이름 변경
  const renameGroup = (groupId: string, groupName: string) => {
    return new Promise((resolve, reject) => {
      renameElementGroup({ robotId: route.query?.projectId as string, groupId, groupName, elementType: 'common' }).then(() => {
        message.success('이름이 변경되었습니다.')
        requestAllElements()
        resolve(true)
      }).catch((err) => {
        reject(err)
      })
    })
  }

  // 삭제분그룹
  const deleteGroup = (groupId: string) => {
    const delElments = elements.value.find(i => i.id === groupId).elements
    delElementGroup({ robotId: route.query?.projectId as string, groupId }).then(() => {
      message.success('삭제되었습니다.')
      requestAllElements()
      // 업데이트프로세스데이터-삭제해당분그룹아래의이미지
      elementDeleteAndUpdateFlow({ elementIds: delElments.map(i => i.id) })
    })
  }
  // 분그룹
  const moveGroup = (originId: string, targetId: string) => {
    moveElement({ robotId: route.query?.projectId as string, elementId: originId, groupId: targetId }).then(() => {
      message.success('이동되었습니다.')
      requestAllElements()
    })
  }

  // 생성요소본
  const elementCopy = (elementId: string) => {
    createElementCopy({ robotId: route.query?.projectId as string, elementId }).then(() => {
      message.success('복사성공')
      requestAllElements()
    })
  }

  const getElementById = (id: string) => {
    const allFlatElements = convertTreeToFlat(elements.value)
    return allFlatElements.find(item => item.id === id)
  }

  const reset = () => {
    elements.value = []
    resetSelectedElement()
    resetCurrentElement()
  }

  const elementsListener = () => {
    BUS.$on('get-elements', () => {
      console.log('on get-elements')
      requestAllElements()
    })
  }

  elementsListener()

  return {
    elements,
    currentElement,
    selectedElement,
    setElements,
    checkName,
    saveElement,
    deleteElement,
    convertTreeToFlat,
    getParentGroup,
    setTempElement,
    requestAllElements,
    requestElementDetail,
    setSelectedElement,
    reset,
    resetCurrentElement,
    renameElement,
    addGroup,
    renameGroup,
    deleteGroup,
    moveGroup,
    elementCopy,
    getElementById,
    addNewElement,
  }
})
