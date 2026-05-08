import type { ITableResponse } from '@/types/normalTable'

import http from './http'

/**
 * @description: 가져오기팀목록
 */
export async function getTeams() {
  const res = await http.post('/api/robot/market-team/get-list')
  return res.data || []
}

/**
 * @description: 가져오기전체사용목록
 */
export async function getAppCards(data) {
  const res = await http.post<ITableResponse>('/api/robot/market-resource/get-all-app-list', data)
  return res.data || { records: [], total: 0 }
}

/**
 * @description: 생성팀
 * @params {type String} teamId 팀Id {type String} appId 사용Id
 */
export function newTeam(data) {
  return http.post('/api/robot/market-team/add', data)
}

/**
 * @description: 생성팀-수검증
 */
export function checkMarketNum() {
  return http.get('/api/robot/quota/check-market-join', { toast: true })
}

/**
 * @description: 색상비밀단계의사용, 가져오기시검증여부예부서내부부서사람원
 */
export function canAchieveApp(data) {
  return http.post('/api/robot/application/use-permission-check', data, { toast: false })
}

/**
 * @description: 신청사용
 */
export function useApplication(data) {
  return http.post('/api/robot/application/submit-use-application', data, { toast: false })
}

/**
 * @description: 가져오기 사용
 */
export function obtainApp(data) {
  return http.post('/api/robot/market-resource/obtain', data, { toast: false })
}

/**
 * @description: 문의사용업데이트상태
 */
export async function getAppUpdateStatus(data) {
  const res = await http.post('/api/robot/market-resource/app-update-check', data)
  return res.data
}

/**
 * @description: 가져오기마켓사용
 */
export function getAppDetails(params: { marketId: string, appId: string }) {
  return http.get('/api/robot/market-resource/app-detail', params)
}

/**
 * @description: 삭제사용
 */
export function deleteApp(params) {
  return http.get('/api/robot/market-resource/delete-app', params)
}

// 메시지목록
export async function messageList(data) {
  const res = await http.post<ITableResponse>('/api/robot/notify/notify-List', data)
  return res.data
}

// 지정완료메시지
export async function setMessageReadById(params) {
  const res = await http.get('/api/robot/notify/set-selected-notify-read', params)
  return res.data
}

// 일완료
export async function setAllRead() {
  const res = await http.get('/api/robot/notify/set-all-notify-read', {})
  return res.data
}

// 추가입력팀
export async function acceptJoinTeam(params) {
  const res = await http.get('/api/robot/notify/accept-join-team', params)
  return res.data
}

// 팀
export async function refuseJoinTeam(params) {
  const res = await http.get('/api/robot/notify/reject-join-team', params)
  return res.data
}

// 가져오기마켓정보
export function teamInfo(params) {
  return http.post('/api/robot/market-team/info', null, { params })
}

// 마켓정보
export function editTeamInfo(data) {
  return http.post('/api/robot/market-team/edit', data)
}

// 열기팀
export function leaveTeamMarket(data) {
  return http.post('/api/robot/market-team/leave', data)
}

// 해제팀
export function dissolveTeamMarket(data) {
  return http.post('/api/robot/market-team/dissolve', data)
}

// 구성원목록
export async function marketUserList(data) {
  const res = await http.post<ITableResponse>('/api/robot/market-user/list', data)
  return res.data || { records: [], total: 0 }
}

// 사용자역할
export function setUserRole(data) {
  return http.post('/api/robot/market-user/role', data)
}

// 제거사용자역할
export function removeUserRole(data) {
  return http.post('/api/robot/market-user/delete', data)
}

// 조회초대요소
export async function getInviteUser(data) {
  const res = await http.post('/api/robot/market-user/get/user', data)
  return res.data
}

// 모든권한조회요소
export async function getTransferUser(data) {
  const res = await http.post('/api/robot/market-user/leave/user', data)
  return res.data
}

// 초대요소
export function inviteMarketUser(data) {
  return http.post('/api/robot/market-user/invite', data)
}

export function generateInviteLink(data: { marketId: string, expireType: string }) {
  return http.post('/api/robot/market-invite/generate-invite-link', data)
}

export function resetInviteLink(data: { marketId: string, expireType: string }) {
  return http.post('/api/robot/market-invite/reset-invite-link', data)
}

// 사용가져오기로사용시이름 변경감지
export function checkAppToRobotName(params) {
  return http.get('/api/market-resource/robot-name-duplicated', params)
}

/**
 * @description: 마켓-추가사용팝업필터링목록
 */
export function getAppFilterLst(data) {
  return http.post('/api/market-resource/add/robot/list', data)
}

/**
 * @description: 마켓-가져오기조직아키텍처정보
 */
export function getCompanyInfo(data) {
  return http.post('/api/robot/market-user/dept/user', data)
}

/**
 * @description: 메시지알림-여부있음새메시지
 */
export async function getNewMessage() {
  const res = await http.get('/api/robot/notify/hasNotify', null, { toast: false })
  return res.data
}

/**
 * @description: 파일다운로드
 * @params {type String} resourceType 유형 mode 방식 본또는단말 resourceName 이름
 */
export function appendixDownload(data: any) {
  return http.post('/api/robot/appendix/download', data)
}

/**
 * @description: 가져오기 파일다운로드
 * @params {type String} resourceType 유형 mode 방식 본또는단말 resourceName 이름
 */
export function cancelAppendixDownload(data: any) {
  return http.post('/api/robot/download/cancel', data)
}

/**
 * @description: 가져오기완료부서의계정목록
 */
export async function getDeployedAccounts(data: any) {
  const res = await http.post<ITableResponse>('/api/robot/market-resource/deployed-user', data)
  return res.data || { records: [], total: 0 }
}

/**
 * @description: 부서마켓사용
 */
export function deployApp(data: any) {
  return http.post('/api/robot/market-resource/deploy', data)
}

/**
 * @description: 버전마켓사용
 */
export function pushApp(data: any) {
  return http.post('/api/robot/market-resource/update/push', data)
}

/**
 * @description: 버전-버전목록조회
 */
export async function getPushHistoryVersions(data: any) {
  const res = await http.post('/api/robot/market-resource/update/version-list', data)
  return res.data
}

// 부서팝업가져오기구성원목록
export async function unDeployUserList(data) {
  const res = await http.post('/api/robot/market-user/undeploy-user', data)
  return res.data
}

// 공유까지마켓 여부필요발송신청조회
export function releaseCheck(data) {
  return http.post('/api/robot/application/pre-release-check', data)
}

// 공유까지마켓 발송위신청
export function releaseApplication(data) {
  return http.post('/api/robot/application/submit-release-application', data)
}

// 공유까지마켓 발송위신청
export function releaseCheckWithPublish(data) {
  return http.post('/api/robot/application/pre-submit-after-publish-check', data)
}

// 다시 발송버전후발송위신청
export function releaseWithPublish(data) {
  return http.post('/api/robot/application/submit-after-publish', data)
}

// 가져오기앱 마켓의신청목록
export function applicationList(params: any) {
  return http.post('/api/robot/application/my-application-page-list', params)
}

// 가져오기앱 마켓의신청목록-삭제
export function deleteApplication(params: object) {
  return http.post('/api/robot/application/my-application-delete', params)
}

// 가져오기앱 마켓의신청목록-판매
export function cancelApplication(params: object) {
  return http.post('/api/robot/application/my-application-cancel', params)
}

// 가져오기마켓사용모든분유형
export async function getAllClassification() {
  return http.get('/api/robot/classification/list')
}
