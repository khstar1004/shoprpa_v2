package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.ListRoleDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * ListRoleDto기기
 * 사용를core패키지아래의ListRoleDto변환로UAP클라이언트의ListRoleDto
 *
 * @author xqcao2
 */
@Component
public class ListRoleDtoMapper {

    /**
     * 를core패키지아래의ListRoleDto변환로UAP클라이언트의ListRoleDto
     *
     * @param listRoleDto core패키지아래의ListRoleDto
     * @return UAP클라이언트의ListRoleDto
     */
    public com.iflytek.sec.uap.client.core.dto.role.ListRoleDto toUapListRoleDto(ListRoleDto listRoleDto) {
        if (listRoleDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.role.ListRoleDto uapListRoleDto =
                new com.iflytek.sec.uap.client.core.dto.role.ListRoleDto();
        // 사용BeanUtils복사속성(패키지PageQueryDto의pageNum및pageSize)
        BeanUtils.copyProperties(listRoleDto, uapListRoleDto);

        return uapListRoleDto;
    }

    /**
     * 를UAP클라이언트의ListRoleDto변환로core패키지아래의ListRoleDto
     *
     * @param uapListRoleDto UAP클라이언트의ListRoleDto
     * @return core패키지아래의ListRoleDto
     */
    public ListRoleDto fromUapListRoleDto(com.iflytek.sec.uap.client.core.dto.role.ListRoleDto uapListRoleDto) {
        if (uapListRoleDto == null) {
            return null;
        }

        ListRoleDto listRoleDto = new ListRoleDto();
        // 사용BeanUtils복사속성(패키지PageQueryDto의pageNum및pageSize)
        BeanUtils.copyProperties(uapListRoleDto, listRoleDto);

        return listRoleDto;
    }
}