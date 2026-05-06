package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.ListUserByRoleDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * ListUserByRoleDto 기기
 * 사용에서 core 의 ListUserByRoleDto 및 UAP 클라이언트의 ListUserByRoleDto 변환
 *
 * 패키지 PageQueryDto 의분필드
 */
@Component
public class ListUserByRoleDtoMapper {

    /**
     * 를 core 패키지아래의 ListUserByRoleDto 변환로 UAP 클라이언트의 ListUserByRoleDto
     *
     * @param listUserByRoleDto core 의 ListUserByRoleDto
     * @return UAP 클라이언트의 ListUserByRoleDto
     */
    public com.iflytek.sec.uap.client.core.dto.user.ListUserByRoleDto toUapListUserByRoleDto(
            ListUserByRoleDto listUserByRoleDto) {
        if (listUserByRoleDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.user.ListUserByRoleDto uapListUserByRoleDto =
                new com.iflytek.sec.uap.client.core.dto.user.ListUserByRoleDto();
        // 복사속성, 패키지 PageQueryDto 의 pageNum, pageSize
        BeanUtils.copyProperties(listUserByRoleDto, uapListUserByRoleDto);

        return uapListUserByRoleDto;
    }

    /**
     * 를 UAP 클라이언트의 ListUserByRoleDto 변환로 core 패키지아래의 ListUserByRoleDto
     *
     * @param uapListUserByRoleDto UAP 클라이언트의 ListUserByRoleDto
     * @return core 의 ListUserByRoleDto
     */
    public ListUserByRoleDto fromUapListUserByRoleDto(
            com.iflytek.sec.uap.client.core.dto.user.ListUserByRoleDto uapListUserByRoleDto) {
        if (uapListUserByRoleDto == null) {
            return null;
        }

        ListUserByRoleDto listUserByRoleDto = new ListUserByRoleDto();
        // 복사속성, 패키지 PageQueryDto 의 pageNum, pageSize
        BeanUtils.copyProperties(uapListUserByRoleDto, listUserByRoleDto);

        return listUserByRoleDto;
    }
}