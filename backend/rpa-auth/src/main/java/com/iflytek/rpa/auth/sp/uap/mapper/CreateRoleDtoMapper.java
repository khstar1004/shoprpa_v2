package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.CreateRoleDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * CreateRoleDto기기
 * 사용를core패키지아래의CreateRoleDto변환로UAP클라이언트의CreateRoleDto
 *
 * @author xqcao2
 */
@Component
public class CreateRoleDtoMapper {

    /**
     * 를core패키지아래의CreateRoleDto변환로UAP클라이언트의CreateRoleDto
     *
     * @param createRoleDto core패키지아래의CreateRoleDto
     * @return UAP클라이언트의CreateRoleDto
     */
    public com.iflytek.sec.uap.client.core.dto.role.CreateRoleDto toUapCreateRoleDto(CreateRoleDto createRoleDto) {
        if (createRoleDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.role.CreateRoleDto uapCreateRoleDto =
                new com.iflytek.sec.uap.client.core.dto.role.CreateRoleDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(createRoleDto, uapCreateRoleDto);

        return uapCreateRoleDto;
    }

    /**
     * 를UAP클라이언트의CreateRoleDto변환로core패키지아래의CreateRoleDto
     *
     * @param uapCreateRoleDto UAP클라이언트의CreateRoleDto
     * @return core패키지아래의CreateRoleDto
     */
    public CreateRoleDto fromUapCreateRoleDto(com.iflytek.sec.uap.client.core.dto.role.CreateRoleDto uapCreateRoleDto) {
        if (uapCreateRoleDto == null) {
            return null;
        }

        CreateRoleDto createRoleDto = new CreateRoleDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapCreateRoleDto, createRoleDto);

        return createRoleDto;
    }
}