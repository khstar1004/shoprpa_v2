import http from './http'

/**
 * AI합치기필요가져오기 결과인증
 * @param data RPA.ConfigParamData
 */
export async function validateContractResult(data: string) {
  const res = await http.post('/scheduler/validate/contract', data)
  return res.data
}