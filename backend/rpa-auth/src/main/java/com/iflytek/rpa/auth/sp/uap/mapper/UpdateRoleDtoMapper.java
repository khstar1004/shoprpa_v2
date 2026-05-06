package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.UpdateRoleDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * UpdateRoleDto기기
 * 사용를core패키지아래의UpdateRoleDto변환로UAP클라이언트의UpdateRoleDto
 *
 * @author xqcao2
 */
@Component
public class UpdateRoleDtoMapper {

    /**
     * 를core패키지아래의UpdateRoleDto변환로UAP클라이언트의UpdateRoleDto
     *
     * @param updateRoleDto core패키지아래의UpdateRoleDto
     * @return UAP클라이언트의UpdateRoleDto
     */
    public com.iflytek.sec.uap.client.core.dto.role.UpdateRoleDto toUapUpdateRoleDto(UpdateRoleDto updateRoleDto) {
        if (updateRoleDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.role.UpdateRoleDto uapUpdateRoleDto =
                new com.iflytek.sec.uap.client.core.dto.role.UpdateRoleDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(updateRoleDto, uapUpdateRoleDto);

        return uapUpdateRoleDto;
    }

    /**
     * 를UAP클라이언트의UpdateRoleDto변환로core패키지아래의UpdateRoleDto
     *
     * @param uapUpdateRoleDto UAP클라이언트의UpdateRoleDto
     * @return core패키지아래의UpdateRoleDto
     */
    public UpdateRoleDto fromUapUpdateRoleDto(com.iflytek.sec.uap.client.core.dto.role.UpdateRoleDto uapUpdateRoleDto) {
        if (uapUpdateRoleDto == null) {
            return null;
        }

        UpdateRoleDto updateRoleDto = new UpdateRoleDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapUpdateRoleDto, updateRoleDto);

        return updateRoleDto;
    }
}