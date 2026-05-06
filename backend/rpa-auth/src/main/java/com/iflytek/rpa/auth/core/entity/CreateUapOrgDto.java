package com.iflytek.rpa.auth.core.entity;

import java.util.List;

/**
 * 생성기기DTO
 * @author xqcao2
 *
 */
public class CreateUapOrgDto {

    /**
     * 기기정보
     */
    private CreateOrgDto uapOrg;

    /**
     * 정보
     */
    private List<UapExtendPropertyDto> extands;

    public CreateOrgDto getUapOrg() {
        return uapOrg;
    }

    public void setUapOrg(CreateOrgDto uapOrg) {
        this.uapOrg = uapOrg;
    }

    public List<UapExtendPropertyDto> getExtands() {
        return extands;
    }

    public void setExtands(List<UapExtendPropertyDto> extands) {
        this.extands = extands;
    }
}