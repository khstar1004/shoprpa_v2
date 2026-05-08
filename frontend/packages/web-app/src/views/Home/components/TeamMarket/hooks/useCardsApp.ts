import { findIndex } from 'lodash-es'
import { reactive, ref } from 'vue'

import { getAllClassification, getAppCards, marketUserList } from '@/api/market'
import type { TableOption } from '@/components/NormalTable'
import { fromIcon } from '@/components/PublishComponents/utils'
import { useCardsTools } from '@/views/Home/components/TeamMarket/hooks/useCardsTools'

import { useRobotUpdate } from './useRobotUpdate'

/**
 * 사용조각사용의사용자 지정Hook
 * @returns {object} 반환패키지첫쪽목록사용, 조각매칭및가져오기팀구성원방법법의대상
 */
export function useCardsApp() {
  // 첫쪽목록컴포넌트의사용
  const homePageListRef = ref(null)
  function refreshHomeTable() {
    if (homePageListRef.value) {
      homePageListRef.value?.fetchTableData()
    }
  }
  // 가져오기 사용업데이트ID의해제값
  const { getInitUpdateIds } = useRobotUpdate('app', homePageListRef)
  /**
   * 가져오기 조각데이터
   * @param {object} params - 요청 매개변수, 패키지marketId대기정보
   * @returns {Promise} 반환일개Promise, 파싱로패키지records및total의대상
   */
  async function getCardsData(params) {
    if (params.marketId) {
      const { total, records } = await getAppCards(params)
      // 가져오기 업데이트ID
      getInitUpdateIds(records)
      return {
        records: records.map(item => ({
          ...item,
          icon: fromIcon(item.url || item.iconUrl).icon,
          color: fromIcon(item.url || item.iconUrl).color,
        })),
        total,
      }
    }
  }

  // 가져오기 조각도구중의테이블단일목록
  const { formList } = useCardsTools()
  // 방식조각매칭대상
  const cardsOption = reactive<TableOption>({
    refresh: false,
    getData: getCardsData, // 가져오기데이터의방법법
    formList, // 테이블단일목록
    params: { // 지정의테이블단일매칭의데이터
      marketId: '', // 마켓ID
      appName: '',
      creatorId: undefined, // 생성자ID
      category: undefined,
      sortKey: 'createTime', // 정렬키
    },
  })
  /**
   * 근거팀가져오기구성원목록
   * @param {string} marketId - 마켓ID
   */
  async function getMembersByTeam(marketId) {
    const { records } = await marketUserList({
      marketId,
      pageNo: 1,
      pageSize: 10000,
    })
    // 생성모든목록, 패키지명및
    const ownerList = records.map(i => ({
      name: `${i.realName || '--'}(${i.phone || '--'})`,
      userId: i.creatorId,
    }))
    // 까지지정creatorId의테이블단일그리고업데이트그선택
    const current = cardsOption.formList[findIndex(cardsOption.formList, { bind: 'creatorId' })]
    current.options = ownerList as unknown as any
  }

  async function getAppCategory() {
    const res = await getAllClassification()
    const current = cardsOption.formList[findIndex(cardsOption.formList, { bind: 'category' })]
    current.options = res.data || []
  }

  // 반환필요의사용및방법법
  return {
    homePageListRef,
    refreshHomeTable,
    cardsOption,
    getMembersByTeam,
    getAppCategory,
  }
}
