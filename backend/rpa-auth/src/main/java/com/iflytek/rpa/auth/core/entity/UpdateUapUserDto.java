package com.iflytek.rpa.auth.core.entity;

import java.util.List;

/**
 * 업데이트사용자
 * @author xqcao2
 *
 */
public class UpdateUapUserDto {

    /**
     * 사용자정보
     */
    private UpdateUserDto user;

    /**
     * 사용자정보
     */
    private List<UapExtendPropertyDto> extands;

    public UpdateUserDto getUser() {
        return user;
    }

    public void setUser(UpdateUserDto user) {
        this.user = user;
    }

    public List<UapExtendPropertyDto> getExtands() {
        return extands;
    }

    public void setExtands(List<UapExtendPropertyDto> extands) {
        this.extands = extands;
    }
}