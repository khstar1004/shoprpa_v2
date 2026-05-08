import { fetchEventSource } from '@microsoft/fetch-event-source'
import type { FetchEventSourceInit } from '@microsoft/fetch-event-source'
import { isFunction } from 'lodash-es'

/**
 * SSE 방식연결
 * @param url 연결주소
 * @param params 매개변수
 * @param options 요청 매칭
 * @param sCB 성공돌아가기조정
 * @param eCB 실패돌아가기조정
 * @returns SSE 요청 제어 객체
 */
function SSERequest(
  url: string,
  params: Record<string, any>,
  options: FetchEventSourceInit,
  sCB: FetchEventSourceInit['onmessage'],
  eCB?: FetchEventSourceInit['onerror'],
) {
  const controller = new AbortController()

  fetchEventSource(url, {
    signal: controller.signal,
    mode: 'cors',
    // mode: 'no-cors',
    headers: {
      'Content-Type': 'application/json',
      'Accept': '*/*',
    },
    ...(options || {}),
    ...(options?.method === 'GET' ? {} : { body: JSON.stringify(params) }),
    onmessage(msg) {
      sCB(msg)
    },
    onerror(err) {
      // 출력오류중지
      isFunction(eCB) && eCB(err)
      throw err
    },
  })

  return controller
};

function get(url: string, callback: FetchEventSourceInit['onmessage'], errorCallback?: FetchEventSourceInit['onerror'], options?: FetchEventSourceInit) {
  return SSERequest(url, null, { method: 'GET', ...options }, callback, errorCallback)
}

function post(url: string, data: Record<string, any>, callback: FetchEventSourceInit['onmessage'], errorCallback?: FetchEventSourceInit['onerror'], options?: FetchEventSourceInit) {
  return SSERequest(url, data, { method: 'POST', ...options }, callback, errorCallback)
}

export const sseRequest = { get, post }
