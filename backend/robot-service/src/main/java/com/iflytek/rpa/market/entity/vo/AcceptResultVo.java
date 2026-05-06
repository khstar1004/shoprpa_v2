package com.iflytek.rpa.market.entity.vo;

import com.iflytek.rpa.utils.response.QuotaCodeEnum;
import lombok.Data;

@Data
public class AcceptResultVo {
    /**
     * S_SUCCESS("000", "성공"),
     * S_REPEAT_JOIN("001", "재복사추가입력"),
     * E_OVER_LIMIT("101", "초과출력위제한"),
     * E_EXPIRE("102", "실패");
     */
    String resultCode;

    public AcceptResultVo(QuotaCodeEnum codeEnum) {
        this.resultCode = codeEnum.getResultCode();
    }

    public AcceptResultVo() {
        resultCode = null;
    }
}