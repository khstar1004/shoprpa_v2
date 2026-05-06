import type {
  DocNode,
  ElementAttrs,
  Message,
  MessageInput,
  MessageOutput,
  ParsedQuestion,
} from '../types'

/**
 * 파싱TiptapEditor의JSON형식로서버필요의형식
 */
export function parseChatContent(editorJson: DocNode): ParsedQuestion {
  const elements: Array<ElementAttrs> = []
  const elementSet = new Set<string>()

  let userText = ''

  // 문서내용
  if (editorJson.content && Array.isArray(editorJson.content)) {
    editorJson.content.forEach((paragraph) => {
      if (paragraph.type === 'paragraph' && paragraph.content) {
        paragraph.content.forEach((node) => {
          if (node.type === 'text') {
            // 통신텍스트
            userText += node.text
          }
          else if (node.type === 'elementNode') {
            // 요소
            const elementNode = node
            const attrs = node.attrs

            const uniqueKey = attrs.elementId || attrs.xpath

            if (!elementSet.has(uniqueKey)) {
              elementSet.add(uniqueKey)
              elements.push(attrs)
            }

            // 에서사용자텍스트중사용반대패키지요소이름
            userText += `\`${elementNode.attrs.name}\``
          }
          else if (node.type === 'descriptionNode') {
            // 설명
            const descriptionNode = node
            // 에서내용중가져오기텍스트
            if (descriptionNode.content && Array.isArray(descriptionNode.content)) {
              descriptionNode.content.forEach((textNode: any) => {
                if (textNode.type === 'text') {
                  userText += textNode.text
                }
              })
            }
          }
          else if (node.type === 'filePathNode') {
            const filePathNode = node
            userText += filePathNode.attrs.path
          }
        })
      }
    })
  }

  return {
    user: userText.trim(),
    elements,
  }
}

/**
 * 를 MessageInput 의 user 텍스트변환로 DocNode
 * user 텍스트형식: 통신텍스트 + `요소이름`( parseChatContent 완료)
 */
export function parseUserTextToDocNode(userText: string, elements: ElementAttrs[]): DocNode {
  // 에서입력의 elements 배열생성요소테이블
  const elementMap = new Map<string, ElementAttrs>()
  elements.forEach((element) => {
    elementMap.set(element.name, element)
  })

  // 행분텍스트
  const lines = userText.split('\n')
  const paragraphs: any[] = []

  lines.forEach((line) => {
    const isBlankLine = !line.trim()

    if (isBlankLine) {
      // 빈행: 예결과위일개있음내용, 생성일개빈;아니오이면건너뛰기
      if (paragraphs.length > 0 && paragraphs[paragraphs.length - 1].content.length > 0) {
        paragraphs.push({
          type: 'paragraph',
          content: [],
        })
      }
      return
    }

    const nodes: any[] = []
    let currentIndex = 0

    // 매칭 `요소이름` 형식( parseChatContent 완료의형식)
    const pattern = /`([^`]+)`/g
    let match = pattern.exec(line)

    while (match !== null) {
      // 추가매칭전의텍스트
      if (match.index > currentIndex) {
        const textBefore = line.substring(currentIndex, match.index)
        if (textBefore) {
          nodes.push({
            type: 'text',
            text: textBefore,
          })
        }
      }

      // 관리매칭까지의요소이름
      const elementName = match[1]
      const elementAttrs = elementMap.get(elementName)

      if (elementAttrs) {
        // 예결과까지완료요소, 생성 elementNode
        nodes.push({
          type: 'elementNode',
          attrs: elementAttrs,
        })
      }
      else {
        // 예결과아니오까지요소, 보관기존로텍스트(패키지반대)
        nodes.push({
          type: 'text',
          text: match[0],
        })
      }

      currentIndex = match.index + match[0].length
      match = pattern.exec(line)
    }

    // 추가의텍스트
    if (currentIndex < line.length) {
      const textAfter = line.substring(currentIndex)
      if (textAfter) {
        nodes.push({
          type: 'text',
          text: textAfter,
        })
      }
    }

    // 예결과현재행있음작업
    if (nodes.length === 0) {
      nodes.push({
        type: 'text',
        text: line,
      })
    }

    // 생성새
    paragraphs.push({
      type: 'paragraph',
      content: nodes,
    })
  })

  // 예결과있음, 생성일개빈
  if (paragraphs.length === 0) {
    paragraphs.push({
      type: 'paragraph',
      content: [],
    })
  }

  return {
    type: 'doc',
    content: paragraphs,
  }
}

/**
 * 파싱서버반환의결과, 변환로TiptapEditor의JSON형식
 */
export function parseOptimizedTextToDocNode(text: string, elements: ElementAttrs[]): DocNode {
  // 가져오기 코드중의내용
  const codeBlockMatch = text.match(/```new_prompt\s*([\s\S]*?)```/)
  const contentText = codeBlockMatch ? codeBlockMatch[1].trim() : text.trim()

  // 에서입력의 elements 배열생성요소테이블
  const elementMap = new Map<string, ElementAttrs>()
  elements.forEach((element) => {
    elementMap.set(element.name, element)
  })

  // 행분텍스트
  const lines = contentText.split('\n')
  const paragraphs: any[] = []

  lines.forEach((line) => {
    const isBlankLine = !line.trim()

    if (isBlankLine) {
      // 빈행: 예결과위일개있음내용, 생성일개빈;아니오이면건너뛰기
      if (paragraphs.length > 0 && paragraphs[paragraphs.length - 1].content.length > 0) {
        paragraphs.push({
          type: 'paragraph',
          content: [],
        })
      }
      return
    }

    const nodes: any[] = []
    let currentIndex = 0

    // 매칭 `{요소이름:elementId}` 또는 `{요소이름}` 및 `[텍스트내용]` 의방식
    const pattern = /`(\{([^}:]+)(?::([^}]+))?\}|\[([^\]]+)\])`/g
    let match = pattern.exec(line)

    while (match !== null) {
      // 추가매칭전의텍스트
      if (match.index > currentIndex) {
        const textBefore = line.substring(currentIndex, match.index)
        if (textBefore) {
          nodes.push({
            type: 'text',
            text: textBefore,
          })
        }
      }

      // 관리매칭까지의내용
      if (match[1].startsWith('{')) {
        // `{요소이름:elementId}` 또는 `{요소이름}` -> elementNode
        const elementName = match[2]
        const elementIdFromText = match[3]

        let elementAttrs = elementMap.get(elementName)

        if (!elementAttrs) {
          elementAttrs = {
            name: elementName,
            imageUrl: '',
            xpath: '',
            outerHtml: '',
            elementId: elementIdFromText || '',
          }
        }

        nodes.push({
          type: 'elementNode',
          attrs: elementAttrs,
        })
      }
      else if (match[1].startsWith('[')) {
        // `[텍스트내용]` -> descriptionNode
        const descriptionContent = match[4]
        nodes.push({
          type: 'descriptionNode',
          content: [
            {
              type: 'text',
              text: descriptionContent,
            },
          ],
        })
      }

      currentIndex = match.index + match[0].length
      match = pattern.exec(line)
    }

    // 추가의텍스트
    if (currentIndex < line.length) {
      const textAfter = line.substring(currentIndex)
      if (textAfter) {
        nodes.push({
          type: 'text',
          text: textAfter,
        })
      }
    }

    // 예결과현재행있음작업
    if (nodes.length === 0) {
      nodes.push({
        type: 'text',
        text: line,
      })
    }

    // 생성새
    paragraphs.push({
      type: 'paragraph',
      content: nodes,
    })
  })

  // 예결과있음, 생성일개빈
  if (paragraphs.length === 0) {
    paragraphs.push({
      type: 'paragraph',
      content: [],
    })
  }

  return {
    type: 'doc',
    content: paragraphs,
  }
}

export function isAssistantMessage(item: Message): item is { role: 'assistant', content: MessageOutput } {
  return item?.role === 'assistant'
}

export function isUserMessage(item: Message): item is { role: 'user', content: MessageInput } {
  return item?.role === 'user'
}