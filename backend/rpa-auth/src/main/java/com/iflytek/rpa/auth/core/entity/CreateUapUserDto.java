package com.iflytek.rpa.auth.core.entity;

import java.util.List;

/**
 * 생성사용자 DTO  본정보으로정보
 * @author xqcao2
 *
 */
public class CreateUapUserDto {

    /**
     * 사용자정보
     */
    private CreateUserDto user;

    /**
     * 사용자정보  있음속성, 필드가능으로아니요관리
     */
    private List<UapExtendPropertyDto> extands;

    public CreateUserDto getUser() {
        return user;
    }

    public void setUser(CreateUserDto user) {
        this.user = user;
    }

    public List<UapExtendPropertyDto> getExtands() {
        return extands;
    }

    public void setExtands(List<UapExtendPropertyDto> extands) {
        this.extands = extands;
    }
}