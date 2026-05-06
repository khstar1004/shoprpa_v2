package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.GetUserDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * GetUserDto 기기
 * 사용에서 core 의 GetUserDto 및 UAP 클라이언트의 GetUserDto 변환
 *
 * 가장자리결과전체일, 패키지이름아니요
 */
@Component
public class GetUserDtoMapper {

    /**
     * 를 core 패키지아래의 GetUserDto 변환로 UAP 클라이언트의 GetUserDto
     *
     * @param getUserDto core 의 GetUserDto
     * @return UAP 클라이언트의 GetUserDto
     */
    public com.iflytek.sec.uap.client.core.dto.user.GetUserDto toUapGetUserDto(GetUserDto getUserDto) {
        if (getUserDto == null) {
            return null;
        }
        com.iflytek.sec.uap.client.core.dto.user.GetUserDto uapGetUserDto =
                new com.iflytek.sec.uap.client.core.dto.user.GetUserDto();
        BeanUtils.copyProperties(getUserDto, uapGetUserDto);
        return uapGetUserDto;
    }

    /**
     * 를 UAP 클라이언트의 GetUserDto 변환로 core 패키지아래의 GetUserDto
     *
     * @param uapGetUserDto UAP 클라이언트의 GetUserDto
     * @return core 의 GetUserDto
     */
    public GetUserDto fromUapGetUserDto(com.iflytek.sec.uap.client.core.dto.user.GetUserDto uapGetUserDto) {
        if (uapGetUserDto == null) {
            return null;
        }
        GetUserDto getUserDto = new GetUserDto();
        BeanUtils.copyProperties(uapGetUserDto, getUserDto);
        return getUserDto;
    }
}